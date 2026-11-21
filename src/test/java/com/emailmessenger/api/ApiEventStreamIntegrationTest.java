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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Deliberately NOT {@code @Transactional}: live updates are built after the
 * poll commits, in a new read transaction, so test data must be committed.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiEventStreamIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserService userService;
    @Autowired UserRepository users;
    @Autowired ConversationKeyService keyService;
    @Autowired OwnerAddressService ownerAddressService;
    @Autowired EmailThreadRepository threads;
    @Autowired MessageRepository messages;
    @Autowired ParticipantRepository participants;
    @Autowired ApplicationEventPublisher events;
    @Autowired ApiEventStream stream;
    @Autowired PlatformTransactionManager txManager;
    @PersistenceContext EntityManager em;

    private final List<Long> createdUsers = new ArrayList<>();
    private String suffix;
    private User owner;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        owner = register("ada-" + suffix + "@example.com");
        token = login(owner.getEmail(), "phone");
    }

    @AfterEach
    void tearDown() {
        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            for (Long id : createdUsers) {
                em.createQuery("select t from EmailThread t where t.owner.id = :id", EmailThread.class)
                        .setParameter("id", id).getResultList().forEach(em::remove);
                em.createNativeQuery("DELETE FROM auth_events WHERE user_id = :id")
                        .setParameter("id", id).executeUpdate();
            }
        });
        createdUsers.forEach(users::deleteById);
    }

    @Test
    void requiresBearerToken() throws Exception {
        mockMvc.perform(get("/api/v1/events")).andExpect(status().isUnauthorized());
    }

    @Test
    void opensWithReadyEventThenSendsChangedRowsAndCounts() throws Exception {
        Participant me = participants.save(new Participant(owner.getEmail(), "Ada"));
        Participant bob = participants.save(new Participant("bob-" + suffix + "@acme.com", "Bob Builder"));
        Participant cy = participants.save(new Participant("cy-" + suffix + "@acme.com", "Cy"));
        String bobKey = inbound("Lunch", bob, me, "pizza at noon?", true, LocalDateTime.of(2026, 11, 20, 9, 0));
        inbound("Report", cy, me, "numbers", false, LocalDateTime.of(2026, 11, 19, 9, 0));

        MvcResult result = mockMvc.perform(get("/api/v1/events").header("Authorization", "Bearer " + token))
                .andExpect(request().asyncStarted())
                .andReturn();
        MockHttpServletResponse response = result.getResponse();
        assertThat(response.getContentType()).startsWith(MediaType.TEXT_EVENT_STREAM_VALUE);
        assertThat(response.getContentAsString()).contains("event:ready\ndata:{}\n\n");

        events.publishEvent(new NewMailEvent(owner.getId(), 1, Set.of(bobKey, "not-a-real-key")));

        JsonNode data = lastData(response.getContentAsString(), "conversations");
        assertThat(data.get("imported").asInt()).isEqualTo(1);
        assertThat(data.get("items")).hasSize(1);
        JsonNode row = data.get("items").get(0);
        assertThat(row.get("key").asText()).isEqualTo(bobKey);
        assertThat(row.get("title").asText()).isEqualTo("Bob Builder");
        assertThat(row.get("preview").asText()).isEqualTo("pizza at noon?");
        assertThat(row.get("unread").asBoolean()).isTrue();
        assertThat(row.get("lastActivityAt").asText()).isEqualTo("2026-11-20T09:00:00Z");
        assertThat(row.get("accounts")).isNotNull();
        assertThat(data.get("counts").get("all").asLong()).isEqualTo(2);
        assertThat(data.get("counts").get("unread").asLong()).isEqualTo(1);
    }

    @Test
    void otherUsersEventsAreNotSent() throws Exception {
        User other = register("eve-" + suffix + "@example.com");
        MvcResult result = mockMvc.perform(get("/api/v1/events").header("Authorization", "Bearer " + token))
                .andExpect(request().asyncStarted())
                .andReturn();

        events.publishEvent(new NewMailEvent(other.getId(), 3, Set.of()));

        assertThat(result.getResponse().getContentAsString()).doesNotContain("event:conversations");
    }

    @Test
    void streamClosesOnceItsTokenIsSignedOut() throws Exception {
        String laptop = login(owner.getEmail(), "laptop");
        mockMvc.perform(get("/api/v1/events").header("Authorization", "Bearer " + token))
                .andExpect(request().asyncStarted());
        assertThat(stream.activeCount(owner.getId())).isEqualTo(1);

        mockMvc.perform(post("/api/v1/account/devices/sign-out-others")
                        .header("Authorization", "Bearer " + laptop))
                .andExpect(status().isOk());
        stream.keepalive();

        assertThat(stream.activeCount(owner.getId())).isZero();
    }

    @Test
    void signingOutTheStreamingTokenItselfAlsoCloses() throws Exception {
        mockMvc.perform(get("/api/v1/events").header("Authorization", "Bearer " + token))
                .andExpect(request().asyncStarted());

        mockMvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        events.publishEvent(new NewMailEvent(owner.getId(), 1, Set.of()));

        assertThat(stream.activeCount(owner.getId())).isZero();
    }

    private User register(String email) {
        User user = userService.register(email, "correct-horse", "Someone");
        createdUsers.add(user.getId());
        return user;
    }

    private String login(String email, String device) throws Exception {
        String json = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ApiDtos.LoginRequest(email, "correct-horse", device))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("token").asText();
    }

    private String inbound(String subject, Participant sender, Participant to, String body,
                           boolean unread, LocalDateTime at) {
        return new TransactionTemplate(txManager).execute(status -> {
            EmailThread thread = threads.save(new EmailThread(owner, subject, "<" + subject + suffix + "@x>"));
            Message m = new Message(thread, sender, subject, body, null, at);
            m.addRecipient(to, RecipientType.TO);
            thread.addMessage(messages.save(m), unread);
            if (!unread) {
                thread.markRead();
            }
            String key = keyService.compute(thread, ownerAddressService.addressesFor(owner));
            thread.setConversationKey(key);
            threads.save(thread);
            return key;
        });
    }

    private JsonNode lastData(String body, String eventName) throws Exception {
        String marker = "event:" + eventName + "\ndata:";
        int at = body.lastIndexOf(marker);
        assertThat(at).as("a %s event in %s", eventName, body).isGreaterThanOrEqualTo(0);
        int start = at + marker.length();
        return objectMapper.readTree(body.substring(start, body.indexOf('\n', start)));
    }
}
