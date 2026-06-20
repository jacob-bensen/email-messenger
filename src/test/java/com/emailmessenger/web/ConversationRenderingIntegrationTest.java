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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Renders the real chat template (/chats/{key}) and pins the layout the UI
 * relies on: the internal-notes popup, the side-by-side reply box + send
 * button, and attachment support.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConversationRenderingIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserService userService;
    @Autowired ConversationKeyService keyService;
    @Autowired OwnerAddressService ownerAddressService;
    @Autowired EmailThreadRepository threadRepository;
    @Autowired MessageRepository messageRepository;
    @Autowired ParticipantRepository participantRepository;

    @MockitoBean ReplyService replyService;

    private String conversationKey;

    @BeforeEach
    void setUp() {
        User owner = userService.register("conv-render@example.com", "password1", "Owner");
        Participant me = participantRepository.save(new Participant("conv-render@example.com", "Owner"));
        Participant ada = participantRepository.save(new Participant("ada-conv@acme.com", "Ada Lovelace"));
        EmailThread thread = threadRepository.save(new EmailThread(owner, "Project Athena", "<a-conv@x>"));
        Message m = new Message(thread, ada, "Project Athena", "First message", null,
                LocalDateTime.of(2026, 1, 1, 9, 0));
        m.addRecipient(me, RecipientType.TO);
        thread.addMessage(messageRepository.save(m), true);
        thread.setConversationKey(keyService.compute(thread, ownerAddressService.addressesFor(owner)));
        threadRepository.saveAndFlush(thread);
        conversationKey = thread.getConversationKey();
    }

    @Test
    @WithMockUser(username = "conv-render@example.com")
    void chatRendersSideBySideReplyWithAttachmentsAndNoInternalNotes() throws Exception {
        String body = mockMvc.perform(get("/chats/{key}", conversationKey))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Reply box and send button sit side by side in a stretch row.
        assertThat(body).contains("class=\"reply-row\"");
        assertThat(body).contains("reply-send");

        // Reply supports file attachments.
        assertThat(body).contains("enctype=\"multipart/form-data\"");
        assertThat(body).contains("name=\"attachments\"");
        assertThat(body).contains("Attach files");

        // Internal notes were removed entirely.
        assertThat(body).doesNotContain("team-notes");
        assertThat(body).doesNotContain("Internal notes");
    }

    @Test
    @WithMockUser(username = "conv-render@example.com")
    void sentReplyWithAttachmentAndNoBodyAppearsAsOutboundBubble() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "attachments", "note.txt", "text/plain", "hi".getBytes());

        // Empty body + an attachment is allowed; ReplyService is mocked so no SMTP.
        mockMvc.perform(multipart("/chats/{key}/send", conversationKey).file(file)
                        .param("body", "").with(csrf()))
                .andExpect(status().is3xxRedirection());

        String body = mockMvc.perform(get("/chats/{key}", conversationKey))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // The sent reply renders as a "you" bubble carrying the attachment chip.
        assertThat(body).contains("bubble-run-me");
        assertThat(body).contains("bubble-me");
        assertThat(body).contains("note.txt");
        assertThat(body).contains(">You<");
    }
}
