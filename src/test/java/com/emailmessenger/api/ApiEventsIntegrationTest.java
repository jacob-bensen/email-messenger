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
import com.emailmessenger.service.ConversationListItem;
import com.emailmessenger.web.ConversationListService;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiEventsIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserService userService;
    @Autowired ConversationKeyService keyService;
    @Autowired OwnerAddressService ownerAddressService;
    @Autowired ConversationListService listService;
    @Autowired ApiEventStream eventStream;
    @Autowired EmailThreadRepository threads;
    @Autowired MessageRepository messages;
    @Autowired ParticipantRepository participants;
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
    void streamNeedsBearerToken() throws Exception {
        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("unauthorized"));
    }

    @Test
    void streamOpensForSignedInUser() throws Exception {
        int before = eventStream.activeCount(owner.getId());

        mockMvc.perform(get("/api/v1/events")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());

        assertThat(eventStream.activeCount(owner.getId())).isEqualTo(before + 1);
    }

    @Test
    void rowsForReturnsOnlyTheChangedConversationsAsUnscopedRows() throws Exception {
        Participant bob = participants.save(new Participant("bob@acme.com", "Bob Builder"));
        Participant cy = participants.save(new Participant("cy@acme.com", "Cy"));
        Participant di = participants.save(new Participant("di@acme.com", "Di"));
        inbound("Old", bob, "older", false, LocalDateTime.of(2026, 1, 1, 9, 0));
        inbound("New", cy, "newest", true, LocalDateTime.of(2026, 1, 2, 9, 0));
        inbound("Untouched", di, "not in the event", true, LocalDateTime.of(2026, 1, 3, 9, 0));
        Set<String> ownerAddresses = ownerAddressService.addressesFor(owner);
        String bobKey = keyService.keyFor(List.of("bob@acme.com"), ownerAddresses);
        String cyKey = keyService.keyFor(List.of("cy@acme.com"), ownerAddresses);

        List<ConversationListItem> rows = listService.rowsFor(owner, Set.of(bobKey, cyKey, "unknown-key"), 50);
        ApiDtos.ConversationsChanged event = ApiDtos.ConversationsChanged.of(
                2, rows, false, listService.counts(owner, null));
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(event));

        assertThat(json.get("imported").asInt()).isEqualTo(2);
        assertThat(json.get("truncated").asBoolean()).isFalse();
        assertThat(json.get("conversations")).hasSize(2);
        assertThat(json.get("conversations").get(0).get("key").asText()).isEqualTo(cyKey);
        assertThat(json.get("conversations").get(0).get("preview").asText()).isEqualTo("newest");
        assertThat(json.get("conversations").get(0).get("unread").asBoolean()).isTrue();
        assertThat(json.get("conversations").get(0).get("lastActivityAt").asText())
                .isEqualTo("2026-01-02T09:00:00Z");
        assertThat(json.get("conversations").get(1).get("key").asText()).isEqualTo(bobKey);
        assertThat(json.get("counts").get("all").asLong()).isEqualTo(3);
        assertThat(json.get("counts").get("unread").asLong()).isEqualTo(2);
    }

    @Test
    void rowsForCapsAtLimit() {
        Participant bob = participants.save(new Participant("bob@acme.com", "Bob"));
        Participant cy = participants.save(new Participant("cy@acme.com", "Cy"));
        inbound("A", bob, "a", false, LocalDateTime.of(2026, 1, 1, 9, 0));
        inbound("B", cy, "b", false, LocalDateTime.of(2026, 1, 2, 9, 0));
        Set<String> ownerAddresses = ownerAddressService.addressesFor(owner);

        List<ConversationListItem> rows = listService.rowsFor(owner, Set.of(
                keyService.keyFor(List.of("bob@acme.com"), ownerAddresses),
                keyService.keyFor(List.of("cy@acme.com"), ownerAddresses)), 1);

        assertThat(rows).singleElement().satisfies(r -> assertThat(r.title()).isEqualTo("Cy"));
    }

    private void inbound(String subject, Participant sender, String body, boolean unread, LocalDateTime at) {
        EmailThread thread = threads.save(new EmailThread(owner, subject, "<" + subject + "@x>"));
        Message m = new Message(thread, sender, subject, body, null, at);
        m.addRecipient(me, RecipientType.TO);
        thread.addMessage(messages.save(m), unread);
        if (!unread) {
            thread.markRead();
        }
        thread.setConversationKey(keyService.compute(thread, ownerAddressService.addressesFor(owner)));
        threads.saveAndFlush(thread);
        em.flush();
        em.clear();
    }
}
