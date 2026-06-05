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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the real Thymeleaf stack and asserts the threads template renders
 * the sender drill-down rail, an active-sender pill, and pagination/search
 * links that round-trip the `from` query param. Covers what
 * `ThreadControllerTest` (standalone MockMvc) can't.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class ThreadInboxRenderingIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserService userService;
    @Autowired EmailThreadRepository threadRepository;
    @Autowired MessageRepository messageRepository;
    @Autowired ParticipantRepository participantRepository;

    @MockBean ReplyService replyService;

    private User user;

    @BeforeEach
    void setUp() {
        user = userService.register("inbox-render@example.com", "password1", "Inbox");
        Participant ada = participantRepository.save(
                new Participant("ada-render@acme.com", "Ada Lovelace"));
        Participant grace = participantRepository.save(
                new Participant("grace-render@navy.mil", "Grace Hopper"));
        EmailThread t1 = threadRepository.save(new EmailThread(user, "Project Athena", "<a-r@x>"));
        EmailThread t2 = threadRepository.save(new EmailThread(user, "Project Olympus", "<o-r@x>"));
        messageRepository.save(new Message(t1, ada, "Project Athena",
                "Ada's first message", null, LocalDateTime.now()));
        messageRepository.save(new Message(t2, grace, "Project Olympus",
                "Grace's first message", null, LocalDateTime.now()));
    }

    @Test
    @WithMockUser(username = "inbox-render@example.com")
    void inboxRendersSenderRailWithBothParticipants() throws Exception {
        String body = mockMvc.perform(get("/threads"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("class=\"sender-rail\"");
        assertThat(body).contains("Everyone");
        assertThat(body).contains("Ada Lovelace");
        assertThat(body).contains("Grace Hopper");
        // The default "Everyone" row is marked active when no sender filter is on.
        assertThat(body).containsPattern("sender-rail-everyone\\s+sender-rail-active");
    }

    @Test
    @WithMockUser(username = "inbox-render@example.com")
    void senderFilterRendersActivePillAndScopesResults() throws Exception {
        String body = mockMvc.perform(get("/threads").param("from", "ada-render@acme.com"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Active-sender pill is present, Everyone link clears the filter.
        assertThat(body).contains("class=\"sender-pill\"");
        assertThat(body).contains("ada-render@acme.com");
        assertThat(body).contains("Show all senders");
        // Only Ada's thread is in the result section title.
        assertThat(body).contains("1 thread from ada-render@acme.com");
        assertThat(body).contains("Project Athena");
        assertThat(body).doesNotContain("Project Olympus");
    }

    @Test
    @WithMockUser(username = "inbox-render@example.com")
    void combinedQueryAndSenderPreservesBothInSearchFormAndLinks() throws Exception {
        String body = mockMvc.perform(get("/threads")
                        .param("q", "athena")
                        .param("from", "ada-render@acme.com"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Hidden `from` input rides along the search form so re-submitting
        // a new query keeps the sender filter in place.
        assertThat(body).contains("name=\"from\" value=\"ada-render@acme.com\"");
        // Clear link is rendered (either q or sender active).
        assertThat(body).contains("class=\"inbox-search-clear\"");
        // Header reflects both filters via the search-query path.
        assertThat(body).contains("&quot;athena&quot;");
    }
}
