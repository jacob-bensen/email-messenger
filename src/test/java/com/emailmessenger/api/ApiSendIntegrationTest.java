package com.emailmessenger.api;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.domain.Message;
import com.emailmessenger.domain.Participant;
import com.emailmessenger.domain.RecipientType;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.OwnerAddressService;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.repository.MailAccountRepository;
import com.emailmessenger.repository.MessageRepository;
import com.emailmessenger.repository.ParticipantRepository;
import com.emailmessenger.service.ConversationKeyService;
import com.emailmessenger.service.ReplyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** docs/API.md §5.8–5.9. SMTP is mocked at {@link ReplyService}; recording is real. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiSendIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserService userService;
    @Autowired ConversationKeyService keyService;
    @Autowired OwnerAddressService ownerAddressService;
    @Autowired EmailThreadRepository threads;
    @Autowired MessageRepository messages;
    @Autowired ParticipantRepository participants;
    @Autowired MailAccountRepository mailAccounts;
    @PersistenceContext EntityManager em;
    @MockitoBean ReplyService replyService;

    private User owner;
    private Participant me;
    private Participant bob;
    private String token;
    private String bobKey;
    private EmailThread lunch;

    @BeforeEach
    void setUp() throws Exception {
        owner = userService.register("ada@example.com", "correct-horse", "Ada Lovelace");
        me = participants.save(new Participant("ada@example.com", "Ada Lovelace"));
        bob = participants.save(new Participant("bob@acme.com", "Bob Builder"));
        String json = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ApiDtos.LoginRequest("ada@example.com", "correct-horse", null))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readTree(json).get("token").asText();
        lunch = inbound("Lunch", bob, List.of(me), "Pizza at noon?", LocalDateTime.of(2026, 11, 16, 9, 30));
        bobKey = lunch.getConversationKey();
    }

    @Test
    void replyContinuesTheLatestThreadAndRecordsAnOutboundBubble() throws Exception {
        long before = messages.count();

        send(bobKey, Map.of("body", "On my way", "asReply", true))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value(bobKey))
                .andExpect(jsonPath("$.threadId").value(lunch.getId()))
                .andExpect(jsonPath("$.messageId").isNumber());

        verify(replyService).sendReply(eq(lunch.getId().longValue()), eq("Lunch"), eq("On my way"),
                eq(List.of()), eq(List.of("bob@acme.com")), eq("ada@example.com"));
        assertThat(messages.count()).isEqualTo(before + 1);
        em.flush();
        em.clear();
        mockMvc.perform(get("/api/v1/conversations/" + bobKey).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.messageCount").value(2))
                .andExpect(jsonPath("$.runs[1].outbound").value(true))
                .andExpect(jsonPath("$.runs[1].messages[0].bodyText").value("On my way"));
    }

    @Test
    void notAReplySendsANewEmailFiledUnderTheSameConversation() throws Exception {
        String json = send(bobKey, Map.of("body", "Separate topic"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value(bobKey))
                .andReturn().getResponse().getContentAsString();

        verify(replyService).sendNewEmail(eq("(no subject)"), eq("Separate topic"), eq(List.of()),
                eq(List.of("bob@acme.com")), eq("ada@example.com"));
        long threadId = objectMapper.readTree(json).get("threadId").asLong();
        assertThat(threadId).isNotEqualTo(lunch.getId());
        em.flush();
        em.clear();
        mockMvc.perform(get("/api/v1/conversations/" + bobKey).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.threadCount").value(2));
    }

    @Test
    void sendsFromTheChosenMailboxWithItsSignature() throws Exception {
        mailAccounts.save(new MailAccount(owner, "imap.example.com", 993, true, "ada@example.com", "cipher"));
        MailAccount work = new MailAccount(owner, "imap.acme.com", 993, true, "work@acme.com", "cipher");
        work.setSignature("Ada\nAcme Corp");
        work = mailAccounts.save(work);
        // Make the conversation visible in the work mailbox's scope.
        Participant workAddress = participants.save(new Participant("work@acme.com", "Work"));
        inbound("Work", bob, List.of(workAddress), "Status?", LocalDateTime.of(2026, 11, 16, 10, 0));

        mockMvc.perform(post("/api/v1/conversations/" + bobKey + "/messages?mailboxId=" + work.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"All good\",\"subject\":\"  Status  \"}"))
                .andExpect(status().isCreated());

        verify(replyService).sendNewEmail(eq("Status"), eq("All good\n\n-- \nAda\nAcme Corp"), eq(List.of()),
                eq(List.of("bob@acme.com")), eq("work@acme.com"));
    }

    @Test
    void rejectsABlankOrMissingBodyWithoutSending() throws Exception {
        send(bobKey, Map.of("body", "   ")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("validation_failed"));
        send(bobKey, Map.of()).andExpect(status().isBadRequest());
        send(bobKey, Map.of("body", "x".repeat(100_001))).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/conversations/" + bobKey + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("validation_failed"));
        verifyNoInteractions(replyService);
    }

    @Test
    void unknownConversationOrForeignMailboxIsNotFound() throws Exception {
        send("0123456789abcdef0123456789abcdef", Map.of("body", "hi"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("not_found"));
        mockMvc.perform(post("/api/v1/conversations/" + bobKey + "/messages?mailboxId=999999")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"hi\"}"))
                .andExpect(status().isNotFound());
        verifyNoInteractions(replyService);
    }

    @Test
    void aConversationWithOnlyYouHasNoRecipients() throws Exception {
        EmailThread note = inbound("Note to self", me, List.of(me), "remember milk",
                LocalDateTime.of(2026, 11, 16, 8, 0));

        send(note.getConversationKey(), Map.of("body", "and eggs"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("no_recipients"));
        verifyNoInteractions(replyService);
    }

    @Test
    void aMailServerFailureIsSendFailedAndRecordsNothing() throws Exception {
        doThrow(new MailSendException("relay down")).when(replyService)
                .sendNewEmail(anyString(), anyString(), anyList(), anyList(), any());
        long before = messages.count();

        send(bobKey, Map.of("body", "hello?"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error.code").value("send_failed"));
        assertThat(messages.count()).isEqualTo(before);
    }

    @Test
    void startsAConversationKeyedByTheRecipients() throws Exception {
        String expected = keyService.keyFor(List.of("cy@acme.com", "dee@acme.com"),
                ownerAddressService.addressesFor(owner));

        mockMvc.perform(post("/api/v1/conversations").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "to", List.of("cy@acme.com; dee@acme.com", "cy@acme.com", "not-an-address"),
                                "subject", "Kickoff",
                                "body", "Monday at 10?"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value(expected));

        verify(replyService).sendNewEmail(eq("Kickoff"), eq("Monday at 10?"), eq(List.of()),
                eq(List.of("cy@acme.com", "dee@acme.com")), eq("ada@example.com"));
        em.flush();
        em.clear();
        mockMvc.perform(get("/api/v1/conversations/" + expected).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.group").value(true))
                .andExpect(jsonPath("$.runs[0].outbound").value(true));
    }

    @Test
    void startingWithoutAValidRecipientIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/conversations").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"to\":[\"nobody\"],\"body\":\"hi\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("validation_failed"));
        mockMvc.perform(post("/api/v1/conversations").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"hi\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(replyService);
    }

    @Test
    void sendingRequiresABearerToken() throws Exception {
        mockMvc.perform(post("/api/v1/conversations/" + bobKey + "/messages")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"hi\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(replyService);
    }

    @Test
    void aRetryWithTheSameIdempotencyKeyReplaysWithoutSendingAgain() throws Exception {
        long before = messages.count();
        String first = sendWithKey(bobKey, "k-1", Map.of("body", "On my way", "asReply", true))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        sendWithKey(bobKey, "k-1", Map.of("body", "On my way", "asReply", true))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andExpect(content().json(first, true));

        verify(replyService, times(1)).sendReply(anyLong(), anyString(), anyString(), anyList(), anyList(), any());
        assertThat(messages.count()).isEqualTo(before + 1);
    }

    @Test
    void theSameKeyForADifferentMessageIsRejectedAndSendsNothing() throws Exception {
        sendWithKey(bobKey, "k-2", Map.of("body", "first")).andExpect(status().isCreated());

        sendWithKey(bobKey, "k-2", Map.of("body", "second"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("idempotency_key_reused"));
        mockMvc.perform(post("/api/v1/conversations").header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "k-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"to\":[\"bob@acme.com\"],\"body\":\"first\"}"))
                .andExpect(status().isUnprocessableEntity());
        verify(replyService, times(1)).sendNewEmail(anyString(), anyString(), anyList(), anyList(), any());
    }

    @Test
    void aFailedSendReleasesTheKeySoTheRetrySends() throws Exception {
        doThrow(new MailSendException("relay down")).doNothing().when(replyService)
                .sendNewEmail(anyString(), anyString(), anyList(), anyList(), any());

        sendWithKey(bobKey, "k-3", Map.of("body", "hello?")).andExpect(status().isBadGateway());
        sendWithKey(bobKey, "k-3", Map.of("body", "hello?"))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Idempotent-Replayed"));
        verify(replyService, times(2)).sendNewEmail(anyString(), anyString(), anyList(), anyList(), any());
    }

    @Test
    void anUnexpectedFailureKeepsTheKeyInProgressRatherThanRiskASecondEmail() throws Exception {
        doThrow(new IllegalStateException("crashed after SMTP")).when(replyService)
                .sendNewEmail(anyString(), anyString(), anyList(), anyList(), any());

        sendWithKey(bobKey, "k-4", Map.of("body", "hello?")).andExpect(status().isInternalServerError());
        sendWithKey(bobKey, "k-4", Map.of("body", "hello?"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("send_in_progress"));
        verify(replyService, times(1)).sendNewEmail(anyString(), anyString(), anyList(), anyList(), any());
    }

    @Test
    void startingAConversationIsIdempotentToo() throws Exception {
        String body = "{\"to\":[\"cy@acme.com\"],\"subject\":\"Kickoff\",\"body\":\"Monday?\"}";
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/v1/conversations").header("Authorization", "Bearer " + token)
                            .header("Idempotency-Key", "6f1c2a7e-3b0d-4c5e-9a8f-1d2e3c4b5a69")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated());
        }
        verify(replyService, times(1)).sendNewEmail(anyString(), anyString(), anyList(), anyList(), any());
    }

    @Test
    void aMalformedIdempotencyKeyIsRejected() throws Exception {
        sendWithKey(bobKey, "has spaces", Map.of("body", "hi"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("validation_failed"));
        sendWithKey(bobKey, "x".repeat(101), Map.of("body", "hi")).andExpect(status().isBadRequest());
        verifyNoInteractions(replyService);
    }

    private ResultActions sendWithKey(String key, String idempotencyKey, Map<String, Object> body) throws Exception {
        return mockMvc.perform(post("/api/v1/conversations/" + key + "/messages")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions send(String key, Map<String, Object> body) throws Exception {
        return mockMvc.perform(post("/api/v1/conversations/" + key + "/messages")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private EmailThread inbound(String subject, Participant sender, List<Participant> to, String body,
                                LocalDateTime at) {
        EmailThread thread = threads.save(new EmailThread(owner, subject, "<" + subject + "@x>"));
        Message m = new Message(thread, sender, subject, body, null, at);
        m.setMessageIdHeader("<" + subject + "@x>");
        for (Participant p : to) {
            m.addRecipient(p, RecipientType.TO);
        }
        thread.addMessage(messages.save(m), true);
        thread.setConversationKey(keyService.compute(thread, ownerAddressService.addressesFor(owner)));
        threads.saveAndFlush(thread);
        em.flush();
        em.clear();
        return thread;
    }
}
