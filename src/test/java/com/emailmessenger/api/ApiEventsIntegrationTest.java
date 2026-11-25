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
import com.emailmessenger.service.ConversationKeyService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * docs/API.md §5.24 end to end. Not {@code @Transactional}: the event is
 * built after the publishing transaction commits, in a new transaction, so
 * the data must really be committed. Rows are removed in {@link #cleanUp()}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiEventsIntegrationTest {

    private static final String EMAIL = "live-owner@events.test";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserService userService;
    @Autowired ConversationKeyService keyService;
    @Autowired OwnerAddressService ownerAddressService;
    @Autowired EmailThreadRepository threads;
    @Autowired MessageRepository messages;
    @Autowired ParticipantRepository participants;
    @Autowired ApplicationEventPublisher events;
    @Autowired TransactionTemplate transactions;
    @Autowired JdbcTemplate jdbc;

    private User owner;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        owner = userService.register(EMAIL, "correct-horse", "Live Owner");
        String json = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ApiDtos.LoginRequest(EMAIL, "correct-horse", null))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readTree(json).get("token").asText();
    }

    @AfterEach
    void cleanUp() {
        Long id = owner.getId();
        jdbc.update("DELETE FROM message_recipients WHERE message_id IN (SELECT m.id FROM messages m "
                + "JOIN email_threads t ON m.thread_id = t.id WHERE t.owner_id = ?)", id);
        jdbc.update("DELETE FROM messages WHERE thread_id IN (SELECT id FROM email_threads WHERE owner_id = ?)", id);
        jdbc.update("DELETE FROM email_threads WHERE owner_id = ?", id);
        jdbc.update("DELETE FROM participants WHERE email LIKE '%@events.test'");
        jdbc.update("DELETE FROM api_tokens WHERE user_id = ?", id);
        jdbc.update("DELETE FROM auth_events WHERE user_id = ?", id);
        jdbc.update("DELETE FROM users WHERE id = ?", id);
    }

    @Test
    void streamsTheChangedRowsAndCountsAfterAnImportCommits() throws Exception {
        MvcResult stream = mockMvc.perform(get("/api/v1/events")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();

        // Two conversations get mail; only one of them is in the event.
        String key = transactions.execute(status -> {
            inbound("Older", "cy@events.test", "Cy", "not part of this event", LocalDateTime.of(2026, 3, 1, 8, 0));
            String changed = inbound("Hello", "ada@events.test", "Ada Lovelace", "are we still on for today?",
                    LocalDateTime.of(2026, 3, 2, 9, 30));
            events.publishEvent(new NewMailEvent(owner.getId(), 1, Set.of(changed)));
            return changed;
        });

        String body = stream.getResponse().getContentAsString();
        assertThat(body).contains("event:conversations-changed");
        String dataLine = body.lines().filter(l -> l.startsWith("data:")).findFirst().orElseThrow();
        JsonNode data = objectMapper.readTree(dataLine.substring("data:".length()));
        assertThat(data.get("items")).hasSize(1);
        JsonNode row = data.get("items").get(0);
        assertThat(row.get("key").asText()).isEqualTo(key);
        assertThat(row.get("title").asText()).isEqualTo("Ada Lovelace");
        assertThat(row.get("preview").asText()).isEqualTo("are we still on for today?");
        assertThat(row.get("unread").asBoolean()).isTrue();
        assertThat(row.get("lastActivityAt").asText()).isEqualTo("2026-03-02T09:30:00Z");
        assertThat(data.get("counts").get("all").asLong()).isEqualTo(2);
        assertThat(data.get("counts").get("unread").asLong()).isEqualTo(2);
        assertThat(data.get("truncated").asBoolean()).isFalse();
    }

    @Test
    void otherUsersEventsAreNotStreamed() throws Exception {
        MvcResult stream = mockMvc.perform(get("/api/v1/events")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();

        transactions.executeWithoutResult(status ->
                events.publishEvent(new NewMailEvent(owner.getId() + 1000, 1, Set.of("whatever"))));

        assertThat(stream.getResponse().getContentAsString()).doesNotContain("conversations-changed");
    }

    @Test
    void withoutATokenItAnswersTheJsonEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/events").accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("unauthorized"));
    }

    private String inbound(String subject, String fromEmail, String fromName, String body, LocalDateTime at) {
        Participant me = participants.findByEmail(EMAIL)
                .orElseGet(() -> participants.save(new Participant(EMAIL, "Live Owner")));
        Participant from = participants.save(new Participant(fromEmail, fromName));
        EmailThread thread = threads.save(new EmailThread(owner, subject, "<" + subject + "@events.test>"));
        Message m = new Message(thread, from, subject, body, null, at);
        m.addRecipient(me, RecipientType.TO);
        thread.addMessage(messages.save(m), true);
        thread.setConversationKey(keyService.compute(thread, ownerAddressService.addressesFor(owner)));
        threads.save(thread);
        return thread.getConversationKey();
    }
}
