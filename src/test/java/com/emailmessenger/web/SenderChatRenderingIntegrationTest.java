package com.emailmessenger.web;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.Message;
import com.emailmessenger.domain.Participant;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.repository.MessageRepository;
import com.emailmessenger.repository.ParticipantRepository;
import com.emailmessenger.service.ReplyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the real Thymeleaf stack and asserts the cross-thread sender chat
 * renders every message from one address with a per-thread badge, and that an
 * unknown sender redirects back to the inbox.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class SenderChatRenderingIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserService userService;
    @Autowired EmailThreadRepository threadRepository;
    @Autowired MessageRepository messageRepository;
    @Autowired ParticipantRepository participantRepository;

    @MockBean ReplyService replyService;

    @BeforeEach
    void setUp() {
        User user = userService.register("sender-chat@example.com", "password1", "Owner");
        Participant ada = participantRepository.save(new Participant("ada-chat@acme.com", "Ada Lovelace"));
        Participant me = participantRepository.save(new Participant("sender-chat@example.com", "Owner"));
        EmailThread athena = threadRepository.save(new EmailThread(user, "Project Athena", "<a-c@x>"));
        EmailThread olympus = threadRepository.save(new EmailThread(user, "Project Olympus", "<o-c@x>"));
        messageRepository.save(new Message(athena, ada, "Project Athena",
                "Athena update one", null, LocalDateTime.of(2026, 1, 1, 9, 0)));
        messageRepository.save(new Message(olympus, ada, "Project Olympus",
                "Olympus kickoff", null, LocalDateTime.of(2026, 1, 2, 9, 0)));
        // An outbound reply the owner sent into Ada's thread.
        Message myReply = new Message(athena, me, "Re: Project Athena",
                "My reply to Ada", null, LocalDateTime.of(2026, 1, 1, 10, 0));
        myReply.markOutbound();
        messageRepository.save(myReply);
    }

    @Test
    @WithMockUser(username = "sender-chat@example.com")
    void senderChatRendersAllThreadsWithBadges() throws Exception {
        String body = mockMvc.perform(get("/senders").param("email", "ada-chat@acme.com"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("Ada Lovelace");
        assertThat(body).contains("class=\"thread-badge\"");
        // Both source threads are badged on the messages.
        assertThat(body).contains("Project Athena");
        assertThat(body).contains("Project Olympus");
        assertThat(body).contains("Athena update one");
        assertThat(body).contains("Olympus kickoff");
    }

    @Test
    @WithMockUser(username = "sender-chat@example.com")
    void senderChatShowsBothSidesIncludingOwnSentReplies() throws Exception {
        String body = mockMvc.perform(get("/senders").param("email", "ada-chat@acme.com"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Ada's received message and the owner's own sent reply both render,
        // with the reply styled as "you".
        assertThat(body).contains("Athena update one");
        assertThat(body).contains("My reply to Ada");
        assertThat(body).contains("bubble-run-me");
        assertThat(body).contains(">You<");
    }

    @Test
    @WithMockUser(username = "sender-chat@example.com")
    void unknownSenderRedirectsToInbox() throws Exception {
        mockMvc.perform(get("/senders").param("email", "nobody@nowhere.test"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/threads"));
    }
}
