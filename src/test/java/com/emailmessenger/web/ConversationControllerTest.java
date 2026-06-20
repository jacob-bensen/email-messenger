package com.emailmessenger.web;

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
import com.emailmessenger.service.ReplyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the real Thymeleaf stack for the unified chats experience: the list at
 * /chats, the 1:1 and group timelines at /chats/{key} (with the signature
 * panel / member dropdowns), and the reply round-trip. ReplyService is mocked
 * so no real email is sent.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConversationControllerTest {

    private static final String GMAIL_SIG =
            "<div class=\"gmail_signature\" data-smartmail=\"gmail_signature\">";

    @Autowired MockMvc mockMvc;
    @Autowired UserService userService;
    @Autowired ConversationKeyService keyService;
    @Autowired OwnerAddressService ownerAddressService;
    @Autowired EmailThreadRepository threads;
    @Autowired MessageRepository messages;
    @Autowired ParticipantRepository participants;

    @MockitoBean ReplyService replyService;

    private User owner;
    private Participant me;
    private String adaKey;
    private String groupKey;

    @BeforeEach
    void setUp() {
        owner = userService.register("chat-user@example.com", "password1", "Owner");
        me = participants.save(new Participant("chat-user@example.com", "Owner"));
        Participant ada = participants.save(new Participant("ada@acme.com", "Ada Lovelace"));
        Participant bob = participants.save(new Participant("bob@acme.com", "Bob Stone"));

        // 1:1 with Ada, carrying a signature.
        adaKey = seed("Invoice", ada, List.of(me),
                "<p>About the invoice.</p>" + GMAIL_SIG + "Ada · Acme · 555-1000</div>",
                LocalDateTime.of(2026, 1, 1, 9, 0));
        // Group with Ada + Bob.
        groupKey = seed("Project", ada, List.of(me, bob),
                "<p>Kickoff Monday.</p>" + GMAIL_SIG + "Ada · Acme</div>",
                LocalDateTime.of(2026, 1, 2, 9, 0));
    }

    @Test
    @WithMockUser(username = "chat-user@example.com")
    void chatsListShowsBothConversations() throws Exception {
        String body = mockMvc.perform(get("/chats"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("class=\"chat-list\"");
        assertThat(body).contains("Ada Lovelace");
        // Group row labels both people.
        assertThat(body).contains("Ada Lovelace, Bob Stone");
        assertThat(body).contains("Kickoff Monday");
    }

    @Test
    @WithMockUser(username = "chat-user@example.com")
    void oneToOneChatShowsTimelineAndPinnedSignature() throws Exception {
        String body = mockMvc.perform(get("/chats/{key}", adaKey))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("About the invoice");
        assertThat(body).contains("class=\"signature-rail\"");
        assertThat(body).contains("555-1000");
        // 1:1 → no group member dropdowns.
        assertThat(body).doesNotContain("class=\"member-list\"");
    }

    @Test
    @WithMockUser(username = "chat-user@example.com")
    void groupChatListsEveryMemberInTheSidePanel() throws Exception {
        String body = mockMvc.perform(get("/chats/{key}", groupKey))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("class=\"member-list\"");
        assertThat(body).contains("Ada Lovelace");
        assertThat(body).contains("Bob Stone");
        // Header reflects a group.
        assertThat(body).contains("2 people");
    }

    @Test
    @WithMockUser(username = "chat-user@example.com")
    void sendingByDefaultPostsANewEmailAndRecordsAYouBubble() throws Exception {
        long before = messages.count();

        mockMvc.perform(post("/chats/{key}/send", adaKey)
                        .param("subject", "Quick question")
                        .param("body", "Are we still on for Friday?")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/chats/" + adaKey));

        // No "reply" flag → a brand-new email with the given subject, recorded as a bubble.
        verify(replyService).sendNewEmail(eq("Quick question"), eq("Are we still on for Friday?"),
                anyList(), anyList(), anyString());
        assertThat(messages.count()).isEqualTo(before + 1);
    }

    @Test
    @WithMockUser(username = "chat-user@example.com")
    void markingAsReplyContinuesTheExistingThread() throws Exception {
        mockMvc.perform(post("/chats/{key}/send", adaKey)
                        .param("body", "On it, thanks!")
                        .param("asReply", "true")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/chats/" + adaKey));

        verify(replyService).sendReply(anyLong(), anyString(), anyString(),
                anyList(), anyList(), anyString());
    }

    @Test
    @WithMockUser(username = "chat-user@example.com")
    void searchFiltersConversationsByParticipant() throws Exception {
        String body = mockMvc.perform(get("/chats").param("q", "Bob"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Only the group (which includes Bob) matches; the Ada 1:1 is filtered out.
        assertThat(body).contains("Bob Stone");
        assertThat(body).contains("Kickoff Monday");
        assertThat(body).doesNotContain("About the invoice");
    }

    @Test
    @WithMockUser(username = "chat-user@example.com")
    void unknownConversationRedirectsToChats() throws Exception {
        mockMvc.perform(get("/chats/{key}", "deadbeefdeadbeefdeadbeefdeadbeef"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/chats"));
    }

    private String seed(String subject, Participant sender, List<Participant> to, String html,
                        LocalDateTime at) {
        EmailThread thread = threads.save(new EmailThread(owner, subject, "<" + subject + "@x>"));
        Message m = new Message(thread, sender, subject, "plain", html, at);
        for (Participant p : to) {
            m.addRecipient(p, RecipientType.TO);
        }
        thread.addMessage(messages.save(m), true);
        thread.setConversationKey(keyService.compute(thread, ownerAddressService.addressesFor(owner)));
        threads.saveAndFlush(thread);
        return thread.getConversationKey();
    }
}
