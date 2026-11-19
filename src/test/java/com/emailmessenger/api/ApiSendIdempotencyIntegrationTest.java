package com.emailmessenger.api;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.ApiSendRequest;
import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.Message;
import com.emailmessenger.domain.Participant;
import com.emailmessenger.domain.RecipientType;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.OwnerAddressService;
import com.emailmessenger.repository.ApiSendRequestRepository;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.repository.MessageRepository;
import com.emailmessenger.repository.ParticipantRepository;
import com.emailmessenger.service.ConversationKeyService;
import com.emailmessenger.service.ReplyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** docs/API.md §5.8.1. SMTP is mocked at {@link ReplyService}; the ledger and recording are real. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiSendIdempotencyIntegrationTest {

    private static final String KEY = "3f6c2a0e-8d7b-4c1e-9a52-1b0f6e7d4c33";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserService userService;
    @Autowired ConversationKeyService keyService;
    @Autowired OwnerAddressService ownerAddressService;
    @Autowired EmailThreadRepository threads;
    @Autowired MessageRepository messages;
    @Autowired ParticipantRepository participants;
    @Autowired ApiSendRequestRepository sendRequests;
    @Autowired Clock clock;
    @PersistenceContext EntityManager em;
    @MockitoBean ReplyService replyService;

    private User owner;
    private String token;
    private String bobKey;

    @BeforeEach
    void setUp() throws Exception {
        owner = userService.register("ada@example.com", "correct-horse", "Ada Lovelace");
        Participant me = participants.save(new Participant("ada@example.com", "Ada Lovelace"));
        Participant bob = participants.save(new Participant("bob@acme.com", "Bob Builder"));
        token = login("ada@example.com", "correct-horse");
        EmailThread lunch = threads.save(new EmailThread(owner, "Lunch", "<lunch@x>"));
        Message m = new Message(lunch, bob, "Lunch", "Pizza at noon?", null, LocalDateTime.of(2026, 11, 16, 9, 30));
        m.setMessageIdHeader("<lunch@x>");
        m.addRecipient(me, RecipientType.TO);
        lunch.addMessage(messages.save(m), true);
        lunch.setConversationKey(keyService.compute(lunch, ownerAddressService.addressesFor(owner)));
        threads.saveAndFlush(lunch);
        em.flush();
        em.clear();
        bobKey = lunch.getConversationKey();
    }

    @Test
    void aRetryWithTheSameKeyReplaysTheFirstResultWithoutSendingAgain() throws Exception {
        long before = messages.count();
        String first = reply(KEY, Map.of("body", "On my way", "asReply", true))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist(ApiConversationController.REPLAYED))
                .andReturn().getResponse().getContentAsString();

        String second = reply(KEY, Map.of("body", "On my way", "asReply", true))
                .andExpect(status().isCreated())
                .andExpect(header().string(ApiConversationController.REPLAYED, "true"))
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(second)).isEqualTo(objectMapper.readTree(first));
        verify(replyService, times(1)).sendReply(Mockito.anyLong(), anyString(), anyString(), anyList(),
                anyList(), any());
        assertThat(messages.count()).isEqualTo(before + 1);
    }

    @Test
    void theSameKeyForADifferentMessageIsRejected() throws Exception {
        reply(KEY, Map.of("body", "First")).andExpect(status().isCreated());

        reply(KEY, Map.of("body", "Second"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("idempotency_key_reused"));
        verify(replyService, times(1)).sendNewEmail(anyString(), anyString(), anyList(), anyList(), any());
    }

    @Test
    void aMailServerFailureReleasesTheKeySoARetrySends() throws Exception {
        doThrow(new MailSendException("relay down")).when(replyService)
                .sendNewEmail(anyString(), anyString(), anyList(), anyList(), any());
        reply(KEY, Map.of("body", "hello?")).andExpect(status().isBadGateway());
        assertThat(sendRequests.findByUserIdAndIdempotencyKey(owner.getId(), KEY)).isEmpty();

        Mockito.reset(replyService);
        reply(KEY, Map.of("body", "hello?"))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist(ApiConversationController.REPLAYED));
        verify(replyService).sendNewEmail(anyString(), anyString(), anyList(), anyList(), any());
    }

    @Test
    void aValidationFailureReleasesTheKey() throws Exception {
        reply(KEY, Map.of("body", "  ")).andExpect(status().isBadRequest());

        assertThat(sendRequests.findByUserIdAndIdempotencyKey(owner.getId(), KEY)).isEmpty();
    }

    @Test
    void anUnfinishedEarlierRequestIsSendInProgress() throws Exception {
        String hash = ApiSendIdempotency.fingerprint(
                java.util.Arrays.asList("reply", bobKey, null, "hi", null, false));
        sendRequests.saveAndFlush(new ApiSendRequest(owner.getId(), KEY, hash, LocalDateTime.now(clock)));

        reply(KEY, Map.of("body", "hi"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("send_in_progress"));
        verifyNoInteractions(replyService);
    }

    @Test
    void aKeyOlderThanItsLifetimeStartsANewSend() throws Exception {
        ApiSendRequest old = new ApiSendRequest(owner.getId(), KEY, "stale",
                LocalDateTime.now(clock).minus(ApiSendIdempotency.KEY_LIFETIME).minusMinutes(1));
        old.complete(bobKey, 1L, 1L, old.getCreatedAt());
        sendRequests.saveAndFlush(old);

        reply(KEY, Map.of("body", "new day")).andExpect(status().isCreated())
                .andExpect(header().doesNotExist(ApiConversationController.REPLAYED));
        verify(replyService).sendNewEmail(anyString(), anyString(), anyList(), anyList(), any());
    }

    @Test
    void keysArePerUser() throws Exception {
        reply(KEY, Map.of("body", "Mine")).andExpect(status().isCreated());
        userService.register("cy@example.com", "battery-staple", "Cy");
        String other = login("cy@example.com", "battery-staple");

        mockMvc.perform(post("/api/v1/conversations").header("Authorization", "Bearer " + other)
                        .header(ApiConversationController.IDEMPOTENCY_KEY, KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"to\":[\"dee@acme.com\"],\"body\":\"Hi\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist(ApiConversationController.REPLAYED));
    }

    @Test
    void startingAConversationIsIdempotentToo() throws Exception {
        String body = "{\"to\":[\"cy@acme.com\"],\"subject\":\"Kickoff\",\"body\":\"Monday?\"}";
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/v1/conversations").header("Authorization", "Bearer " + token)
                            .header(ApiConversationController.IDEMPOTENCY_KEY, KEY)
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated());
        }
        verify(replyService, times(1)).sendNewEmail(anyString(), anyString(), anyList(), anyList(), any());

        mockMvc.perform(post("/api/v1/conversations").header("Authorization", "Bearer " + token)
                        .header(ApiConversationController.IDEMPOTENCY_KEY, KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"to\":[\"dee@acme.com\"],\"subject\":\"Kickoff\",\"body\":\"Monday?\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void aMalformedKeyIsRejectedWithoutSending() throws Exception {
        reply("has spaces", Map.of("body", "hi")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("validation_failed"));
        reply("x".repeat(101), Map.of("body", "hi")).andExpect(status().isBadRequest());
        verifyNoInteractions(replyService);
    }

    @Test
    void fingerprintSeparatesFieldBoundaries() {
        assertThat(ApiSendIdempotency.fingerprint(List.of("ab", "c")))
                .isNotEqualTo(ApiSendIdempotency.fingerprint(List.of("a", "bc")));
        assertThat(ApiSendIdempotency.fingerprint(java.util.Arrays.asList("a", null)))
                .isNotEqualTo(ApiSendIdempotency.fingerprint(List.of("a", "null")));
    }

    private ResultActions reply(String idempotencyKey, Map<String, Object> body) throws Exception {
        return mockMvc.perform(post("/api/v1/conversations/" + bobKey + "/messages")
                .header("Authorization", "Bearer " + token)
                .header(ApiConversationController.IDEMPOTENCY_KEY, idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private String login(String email, String password) throws Exception {
        String json = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApiDtos.LoginRequest(email, password, null))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("token").asText();
    }
}
