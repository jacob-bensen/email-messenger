package com.emailmessenger.api;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.Message;
import com.emailmessenger.domain.Participant;
import com.emailmessenger.domain.RecipientType;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.NewMailEvent;
import com.emailmessenger.email.OwnerAddressService;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.repository.MessageRepository;
import com.emailmessenger.repository.ParticipantRepository;
import com.emailmessenger.repository.UserRepository;
import com.emailmessenger.service.ConversationKeyService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * docs/API.md §5.24 over a real HTTP connection. Deliberately NOT
 * {@code @Transactional}: the event is built in its own transaction after the
 * poll commits, and the stream's async dispatch must pass the API chain.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiEventsIntegrationTest {

    @LocalServerPort int port;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserService userService;
    @Autowired UserRepository users;
    @Autowired ConversationKeyService keyService;
    @Autowired OwnerAddressService ownerAddressService;
    @Autowired EmailThreadRepository threads;
    @Autowired MessageRepository messages;
    @Autowired ParticipantRepository participants;
    @Autowired ApiEventStream stream;
    @Autowired ApplicationEventPublisher events;
    @Autowired TransactionTemplate tx;
    @Autowired JdbcTemplate jdbc;

    private final HttpClient http = HttpClient.newHttpClient();
    private User owner;
    private Long threadId;
    private Long meId;
    private Long senderId;
    private String key;

    @BeforeEach
    void setUp() {
        owner = userService.register("events-owner@example.com", "correct-horse", "Events Owner");
        tx.executeWithoutResult(status -> {
            Participant me = participants.save(new Participant("events-owner@example.com", "Events Owner"));
            Participant grace = participants.save(new Participant("events-grace@acme.com", "Grace Hopper"));
            meId = me.getId();
            senderId = grace.getId();
            EmailThread thread = threads.save(new EmailThread(owner, "Compilers", "<events-compilers@x>"));
            Message m = new Message(thread, grace, "Compilers", "the first one works", null,
                    LocalDateTime.of(2026, 11, 25, 8, 15));
            m.addRecipient(me, RecipientType.TO);
            thread.addMessage(messages.save(m), true);
            thread.setConversationKey(keyService.compute(thread, ownerAddressService.addressesFor(owner)));
            threads.save(thread);
            threadId = thread.getId();
            key = thread.getConversationKey();
        });
    }

    @AfterEach
    void tearDown() {
        tx.executeWithoutResult(status -> {
            threads.deleteById(threadId);
            participants.deleteById(meId);
            participants.deleteById(senderId);
        });
        // Sign-in and registration leave rows that reference the user.
        for (String table : new String[] {"auth_events", "email_verifications", "digest_email_preferences",
                "subscriptions", "api_tokens"}) {
            jdbc.update("DELETE FROM " + table + " WHERE user_id = ?", owner.getId());
        }
        users.deleteById(owner.getId());
    }

    @Test
    void streamCarriesTheChangedRowsAndCounts() throws Exception {
        String token = login();
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/events"))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "text/event-stream")
                .GET().build();
        HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
                v -> assertThat(v).startsWith("text/event-stream"));
        awaitSubscribers(1);

        // Published outside any transaction, as the scheduler's poll path does.
        events.publishEvent(new NewMailEvent(owner.getId(), 1, Set.of(key, "no-such-key")));

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String[] event = CompletableFuture.supplyAsync(() -> readEvent(reader))
                    .get(10, TimeUnit.SECONDS);
            assertThat(event[0]).isEqualTo("conversations-changed");
            JsonNode data = objectMapper.readTree(event[1]);
            assertThat(data.get("imported").asInt()).isEqualTo(1);
            assertThat(data.get("conversations")).hasSize(1);
            JsonNode row = data.get("conversations").get(0);
            assertThat(row.get("key").asText()).isEqualTo(key);
            assertThat(row.get("title").asText()).isEqualTo("Grace Hopper");
            assertThat(row.get("preview").asText()).isEqualTo("the first one works");
            assertThat(row.get("unread").asBoolean()).isTrue();
            assertThat(row.get("lastActivityAt").asText()).isEqualTo("2026-11-25T08:15:00Z");
            assertThat(row.get("accounts").isArray()).isTrue();
            assertThat(data.get("counts").get("all").asInt()).isEqualTo(1);
            assertThat(data.get("counts").get("unread").asInt()).isEqualTo(1);
        }
    }

    @Test
    void streamNeedsABearerToken() throws Exception {
        HttpResponse<String> response = http.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/events")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(objectMapper.readTree(response.body()).at("/error/code").asText()).isEqualTo("unauthorized");
    }

    private String login() throws Exception {
        HttpResponse<String> response = http.send(HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"email\":\"events-owner@example.com\",\"password\":\"correct-horse\"}"))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        return objectMapper.readTree(response.body()).get("token").asText();
    }

    private void awaitSubscribers(int n) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (stream.activeCount(owner.getId()) < n && System.nanoTime() < deadline) {
            Thread.sleep(20);
        }
        assertThat(stream.activeCount(owner.getId())).isEqualTo(n);
    }

    /** The next named event as {name, data}, skipping comment lines. */
    private static String[] readEvent(BufferedReader reader) {
        try {
            String name = null;
            String data = null;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("event:")) {
                    name = line.substring(6).trim();
                } else if (line.startsWith("data:")) {
                    data = line.substring(5).trim();
                } else if (line.isEmpty() && name != null) {
                    return new String[] {name, data};
                }
            }
            throw new IllegalStateException("stream ended");
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
