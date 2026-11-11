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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The JSON chats list must report exactly what {@code GET /chats} renders. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConversationApiControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserService userService;
    @Autowired ApiTokenService tokenService;
    @Autowired ConversationKeyService keyService;
    @Autowired OwnerAddressService ownerAddressService;
    @Autowired EmailThreadRepository threads;
    @Autowired MessageRepository messages;
    @Autowired ParticipantRepository participants;

    private User owner;
    private String token;
    private String adaKey;
    private String groupKey;

    @BeforeEach
    void setUp() {
        owner = userService.register("api-chats@example.com", "password1", "Owner");
        token = tokenService.issue(owner, "test").token();
        Participant me = participants.save(new Participant("api-chats@example.com", "Owner"));
        Participant ada = participants.save(new Participant("ada@acme.com", "Ada Lovelace"));
        Participant bob = participants.save(new Participant("bob@acme.com", "Bob Stone"));

        adaKey = seed(owner, "Invoice", ada, List.of(me), "<p>About the   invoice.</p>",
                LocalDateTime.of(2026, 1, 1, 9, 0), false);
        groupKey = seed(owner, "Project", ada, List.of(me, bob), "<p>Kickoff Monday.</p>",
                LocalDateTime.of(2026, 1, 2, 9, 30, 15), true);
    }

    private MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder request) {
        return request.header("Authorization", "Bearer " + token);
    }

    @Test
    void listsConversationsMostRecentFirstWithCounts() throws Exception {
        mockMvc.perform(authed(get("/api/v1/conversations")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].key").value(groupKey))
                .andExpect(jsonPath("$.items[0].title").value("Ada Lovelace, Bob Stone"))
                .andExpect(jsonPath("$.items[0].group").value(true))
                .andExpect(jsonPath("$.items[0].memberCount").value(2))
                .andExpect(jsonPath("$.items[0].preview").value("Kickoff Monday."))
                .andExpect(jsonPath("$.items[0].lastActivityAt").value("2026-01-02T09:30:15Z"))
                .andExpect(jsonPath("$.items[0].unread").value(true))
                .andExpect(jsonPath("$.items[0].threadCount").value(1))
                .andExpect(jsonPath("$.items[0].accounts").isArray())
                .andExpect(jsonPath("$.items[0].senderEmail").value("ada@acme.com"))
                .andExpect(jsonPath("$.items[1].key").value(adaKey))
                .andExpect(jsonPath("$.items[1].title").value("Ada Lovelace"))
                .andExpect(jsonPath("$.items[1].initials").value("AL"))
                .andExpect(jsonPath("$.items[1].group").value(false))
                .andExpect(jsonPath("$.items[1].preview").value("About the invoice."))
                .andExpect(jsonPath("$.items[1].unread").value(false))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.pageSize").value(30))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.counts.all").value(2))
                .andExpect(jsonPath("$.counts.unread").value(1))
                .andExpect(jsonPath("$.counts.awaiting").isNumber())
                .andExpect(jsonPath("$.counts.attachments").value(0));
    }

    @Test
    void unreadFilterNarrowsTheListButNotTheCounts() throws Exception {
        mockMvc.perform(authed(get("/api/v1/conversations").param("filter", "unread")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].key").value(groupKey))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.counts.all").value(2));
    }

    @Test
    void unknownFilterFallsBackToAllLikeTheWebsite() throws Exception {
        mockMvc.perform(authed(get("/api/v1/conversations").param("filter", "bogus")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2));
    }

    @Test
    void searchMatchesLikeTheWebsite() throws Exception {
        mockMvc.perform(authed(get("/api/v1/conversations").param("q", "project")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].key").value(groupKey));
    }

    @Test
    void pagePastTheEndIsEmptyAndNegativePageClampsToZero() throws Exception {
        mockMvc.perform(authed(get("/api/v1/conversations").param("page", "5")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.page").value(5))
                .andExpect(jsonPath("$.totalItems").value(2));

        mockMvc.perform(authed(get("/api/v1/conversations").param("page", "-3")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void nonNumericPageIs400() throws Exception {
        mockMvc.perform(authed(get("/api/v1/conversations").param("page", "abc")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("invalid_request"));
    }

    @Test
    void neverReturnsAnotherUsersConversations() throws Exception {
        User other = userService.register("someone-else@example.com", "password1", null);
        Participant carol = participants.save(new Participant("carol@acme.com", "Carol"));
        Participant them = participants.save(new Participant("someone-else@example.com", null));
        seed(other, "Private", carol, List.of(them), "<p>Secret.</p>",
                LocalDateTime.of(2026, 2, 1, 9, 0), true);

        mockMvc.perform(authed(get("/api/v1/conversations")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.counts.all").value(2));
    }

    @Test
    void requiresAToken() throws Exception {
        mockMvc.perform(get("/api/v1/conversations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("unauthorized"));
    }

    private String seed(User threadOwner, String subject, Participant sender, List<Participant> to,
                        String html, LocalDateTime at, boolean unread) {
        EmailThread thread = threads.save(new EmailThread(threadOwner, subject, "<" + subject + "@x>"));
        Message m = new Message(thread, sender, subject, "plain", html, at);
        for (Participant p : to) {
            m.addRecipient(p, RecipientType.TO);
        }
        thread.addMessage(messages.save(m));
        if (!unread) {
            thread.markRead();
        }
        thread.setConversationKey(
                keyService.compute(thread, ownerAddressService.addressesFor(threadOwner)));
        threads.saveAndFlush(thread);
        return thread.getConversationKey();
    }
}
