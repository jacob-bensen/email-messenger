package com.emailmessenger.api;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.Message;
import com.emailmessenger.domain.Participant;
import com.emailmessenger.domain.RecipientType;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.OwnerAddressService;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.repository.MessageRepository;
import com.emailmessenger.repository.ParticipantRepository;
import com.emailmessenger.service.ConversationKeyService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** docs/API.md §5.21. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiEventsIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserService userService;
    @Autowired ConversationKeyService keyService;
    @Autowired OwnerAddressService ownerAddressService;
    @Autowired EmailThreadRepository threads;
    @Autowired MessageRepository messages;
    @Autowired ParticipantRepository participants;
    @Autowired ApiEventPayloads payloads;
    @Autowired ApiTokenService tokens;
    @PersistenceContext EntityManager em;

    private User owner;
    private Participant me;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        owner = userService.register("ada@example.com", "correct-horse", "Ada Lovelace");
        me = participants.save(new Participant("ada@example.com", "Ada Lovelace"));
        String json = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ApiDtos.LoginRequest("ada@example.com", "correct-horse", null))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readTree(json).get("token").asText();
    }

    @Test
    void streamOpensWithReady() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/events")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();

        assertThat(result.getResponse().getContentType()).startsWith(MediaType.TEXT_EVENT_STREAM_VALUE);
        assertThat(result.getResponse().getContentAsString()).contains("event:ready\ndata:{}\n\n");
    }

    @Test
    void streamNeedsAToken() throws Exception {
        mockMvc.perform(get("/api/v1/events").accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("unauthorized"));
    }

    @Test
    void payloadHasTheChangedRowsNewestFirstAndUnscopedCounts() throws Exception {
        Participant bob = participants.save(new Participant("bob@acme.com", "Bob Builder"));
        Participant cy = participants.save(new Participant("cy@acme.com", "Cy"));
        Participant dee = participants.save(new Participant("dee@acme.com", "Dee"));
        String bobKey = inbound("Old", bob, "older", false, LocalDateTime.of(2026, 1, 1, 9, 0));
        String cyKey = inbound("New", cy, "newest", true, LocalDateTime.of(2026, 1, 2, 9, 30));
        inbound("Untouched", dee, "not part of the event", true, LocalDateTime.of(2026, 1, 3, 9, 0));

        ApiDtos.ConversationsEvent event = payloads
                .conversationsChanged(owner.getId(), 2, Set.of(bobKey, cyKey, "no-such-key"))
                .orElseThrow();

        assertThat(event.imported()).isEqualTo(2);
        assertThat(event.truncated()).isFalse();
        assertThat(event.conversations()).extracting(ApiDtos.ConversationRow::key).containsExactly(cyKey, bobKey);
        assertThat(event.conversations().get(0).preview()).isEqualTo("newest");
        assertThat(event.conversations().get(0).unread()).isTrue();
        assertThat(event.counts()).isEqualTo(new ApiDtos.CountsResponse(3, 2, 3, 0));

        JsonNode wire = objectMapper.readTree(objectMapper.writeValueAsString(event));
        assertThat(wire.get("conversations").get(0).get("lastActivityAt").asText())
                .isEqualTo("2026-01-02T09:30:00Z");
        assertThat(wire.get("counts").get("all").asLong()).isEqualTo(3);
    }

    @Test
    void moreThanFiftyChangedConversationsIsTruncated() {
        Set<String> keys = new HashSet<>();
        for (int i = 0; i <= ApiEventPayloads.MAX_ROWS; i++) {
            keys.add("key-" + i);
        }

        ApiDtos.ConversationsEvent event = payloads.conversationsChanged(owner.getId(), 60, keys).orElseThrow();

        assertThat(event.truncated()).isTrue();
        assertThat(event.conversations()).isEmpty();
    }

    @Test
    void keepaliveTokenCheckSeesRevocation() throws Exception {
        String json = mockMvc.perform(get("/api/v1/account/devices").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long tokenId = objectMapper.readTree(json).get("items").get(0).get("id").asLong();

        assertThat(tokens.isActive(tokenId)).isTrue();
        tokens.revoke(tokenId);
        assertThat(tokens.isActive(tokenId)).isFalse();
        assertThat(tokens.isActive(Long.MAX_VALUE)).isFalse();
    }

    private String inbound(String subject, Participant sender, String body, boolean unread, LocalDateTime at) {
        EmailThread thread = threads.save(new EmailThread(owner, subject, "<" + subject + "@x>"));
        Message m = new Message(thread, sender, subject, body, null, at);
        m.addRecipient(me, RecipientType.TO);
        thread.addMessage(messages.save(m), unread);
        if (!unread) {
            thread.markRead();
        }
        String key = keyService.compute(thread, ownerAddressService.addressesFor(owner));
        thread.setConversationKey(key);
        threads.saveAndFlush(thread);
        em.flush();
        em.clear();
        return key;
    }
}
