package com.emailmessenger.api;

import com.emailmessenger.auth.ApiTokenService;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The JSON chats list + timeline, authenticated with a real bearer token. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiConversationControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserService userService;
    @Autowired ApiTokenService apiTokens;
    @Autowired ConversationKeyService keyService;
    @Autowired OwnerAddressService ownerAddressService;
    @Autowired EmailThreadRepository threads;
    @Autowired MessageRepository messages;
    @Autowired ParticipantRepository participants;

    private User owner;
    private String bearer;
    private String adaKey;
    private String groupKey;
    private EmailThread adaThread;

    @BeforeEach
    void setUp() {
        owner = userService.register("api-chat@example.com", "password1", "Owner");
        bearer = "Bearer " + apiTokens.issue(owner, "test").token();
        Participant me = participants.save(new Participant("api-chat@example.com", "Owner"));
        Participant ada = participants.save(new Participant("ada@acme.com", "Ada Lovelace"));
        Participant bob = participants.save(new Participant("bob@acme.com", "Bob Stone"));

        adaThread = seed(owner, "Invoice", ada, List.of(me),
                "<p>About the invoice.</p><p>Second line.</p>", LocalDateTime.of(2026, 1, 1, 9, 0));
        adaKey = adaThread.getConversationKey();
        groupKey = seed(owner, "Project", ada, List.of(me, bob),
                "<p>Kickoff Monday.</p>", LocalDateTime.of(2026, 1, 2, 9, 0)).getConversationKey();
    }

    @Test
    void listIsMostRecentFirstWithPagingEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/conversations").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(30))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].key").value(groupKey))
                .andExpect(jsonPath("$.items[0].title").value("Ada Lovelace, Bob Stone"))
                .andExpect(jsonPath("$.items[0].group").value(true))
                .andExpect(jsonPath("$.items[0].memberCount").value(2))
                .andExpect(jsonPath("$.items[0].preview").value("Kickoff Monday."))
                .andExpect(jsonPath("$.items[0].lastActivityAt").value("2026-01-02T09:00:00Z"))
                .andExpect(jsonPath("$.items[0].unread").value(true))
                .andExpect(jsonPath("$.items[0].senderEmail").value("ada@acme.com"))
                .andExpect(jsonPath("$.items[0].accounts").isArray())
                .andExpect(jsonPath("$.items[1].key").value(adaKey))
                .andExpect(jsonPath("$.items[1].group").value(false));
    }

    @Test
    void listHonoursSizeQueryAndFilter() throws Exception {
        mockMvc.perform(get("/api/v1/conversations?size=1").header("Authorization", bearer))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get("/api/v1/conversations?q=invoice").header("Authorization", bearer))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].key").value(adaKey));

        adaThread.markRead();
        threads.saveAndFlush(adaThread);
        mockMvc.perform(get("/api/v1/conversations?filter=unread").header("Authorization", bearer))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].key").value(groupKey));
    }

    @Test
    void detailFlattensTheTimelineIntoMessages() throws Exception {
        mockMvc.perform(get("/api/v1/conversations/{key}", adaKey).header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value(adaKey))
                .andExpect(jsonPath("$.title").value("Ada Lovelace"))
                .andExpect(jsonPath("$.group").value(false))
                .andExpect(jsonPath("$.threadCount").value(1))
                .andExpect(jsonPath("$.members.length()").value(1))
                .andExpect(jsonPath("$.members[0].email").value("ada@acme.com"))
                .andExpect(jsonPath("$.members[0].label").value("Ada Lovelace"))
                .andExpect(jsonPath("$.messages.length()").value(1))
                .andExpect(jsonPath("$.messages[0].threadId").value(adaThread.getId()))
                .andExpect(jsonPath("$.messages[0].threadSubject").value("Invoice"))
                .andExpect(jsonPath("$.messages[0].sender.email").value("ada@acme.com"))
                .andExpect(jsonPath("$.messages[0].outbound").value(false))
                .andExpect(jsonPath("$.messages[0].sentAt").value("2026-01-01T09:00:00Z"))
                .andExpect(jsonPath("$.messages[0].bodyText").value("About the invoice.\nSecond line."))
                .andExpect(jsonPath("$.messages[0].attachments").isEmpty());
    }

    @Test
    void fetchingADetailDoesNotMarkItReadButTheReadEndpointDoes() throws Exception {
        mockMvc.perform(get("/api/v1/conversations/{key}", adaKey).header("Authorization", bearer))
                .andExpect(status().isOk());
        assertThat(threads.findById(adaThread.getId()).orElseThrow().isUnread()).isTrue();

        mockMvc.perform(post("/api/v1/conversations/{key}/read", adaKey).header("Authorization", bearer))
                .andExpect(status().isNoContent());
        assertThat(threads.findById(adaThread.getId()).orElseThrow().isUnread()).isFalse();
    }

    @Test
    void unknownKeyIs404() throws Exception {
        mockMvc.perform(get("/api/v1/conversations/nope").header("Authorization", bearer))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("not_found"));
        mockMvc.perform(post("/api/v1/conversations/nope/read").header("Authorization", bearer))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("not_found"));
    }

    @Test
    void anotherUsersConversationIsInvisible() throws Exception {
        User stranger = userService.register("stranger@example.com", "password1", "Stranger");
        String strangerBearer = "Bearer " + apiTokens.issue(stranger, "test").token();

        mockMvc.perform(get("/api/v1/conversations").header("Authorization", strangerBearer))
                .andExpect(jsonPath("$.totalItems").value(0));
        mockMvc.perform(get("/api/v1/conversations/{key}", adaKey).header("Authorization", strangerBearer))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/conversations/{key}/read", adaKey).header("Authorization", strangerBearer))
                .andExpect(status().isNotFound());
        assertThat(threads.findById(adaThread.getId()).orElseThrow().isUnread()).isTrue();
    }

    @Test
    void unauthenticatedIs401() throws Exception {
        mockMvc.perform(get("/api/v1/conversations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("unauthorized"));
    }

    private EmailThread seed(User user, String subject, Participant sender, List<Participant> to,
                             String html, LocalDateTime at) {
        EmailThread thread = threads.save(new EmailThread(user, subject, "<" + subject + "@api>"));
        Message m = new Message(thread, sender, subject, "plain", html, at);
        for (Participant p : to) {
            m.addRecipient(p, RecipientType.TO);
        }
        thread.addMessage(messages.save(m), true);
        thread.setConversationKey(keyService.compute(thread, ownerAddressService.addressesFor(user)));
        return threads.saveAndFlush(thread);
    }
}
