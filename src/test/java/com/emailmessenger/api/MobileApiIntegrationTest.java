package com.emailmessenger.api;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.Attachment;
import com.emailmessenger.domain.AuthEventType;
import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.Message;
import com.emailmessenger.domain.Participant;
import com.emailmessenger.domain.RecipientType;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.OwnerAddressService;
import com.emailmessenger.repository.ApiTokenRepository;
import com.emailmessenger.repository.AuthEventRepository;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.repository.MessageRepository;
import com.emailmessenger.repository.ParticipantRepository;
import com.emailmessenger.service.ConversationKeyService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pins the /api/v1 contract in docs/API.md end to end through the real security chain. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MobileApiIntegrationTest {

    private static final String EMAIL = "api-owner@example.com";
    private static final String PASSWORD = "password1";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper json;
    @Autowired UserService userService;
    @Autowired ConversationKeyService keyService;
    @Autowired OwnerAddressService ownerAddressService;
    @Autowired EmailThreadRepository threadRepository;
    @Autowired MessageRepository messageRepository;
    @Autowired ParticipantRepository participantRepository;
    @Autowired ApiTokenRepository apiTokens;
    @Autowired AuthEventRepository authEvents;

    private String conversationKey;

    @BeforeEach
    void setUp() {
        User owner = userService.register(EMAIL, PASSWORD, "Owner");
        Participant me = participantRepository.save(new Participant(EMAIL, "Owner"));
        Participant ada = participantRepository.save(new Participant("ada-api@acme.com", "Ada Lovelace"));
        EmailThread thread = threadRepository.save(new EmailThread(owner, "Project Athena", "<a-api@x>"));
        Message m = new Message(thread, ada, "Project Athena", "First message", null,
                LocalDateTime.of(2026, 1, 1, 9, 0));
        m.addRecipient(me, RecipientType.TO);
        m.addAttachment(new Attachment(m, "spec.pdf", "application/pdf", 1234L, null));
        thread.addMessage(messageRepository.save(m), true);
        thread.setConversationKey(keyService.compute(thread, ownerAddressService.addressesFor(owner)));
        threadRepository.saveAndFlush(thread);
        conversationKey = thread.getConversationKey();
    }

    private String login() throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    @Test
    void loginReturnsTokenAndUserAndStoresOnlyTheHash() throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\" API-Owner@Example.com \",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value(EMAIL))
                .andExpect(jsonPath("$.user.displayName").value("Owner"))
                .andExpect(jsonPath("$.user.emailVerified").value(false))
                .andExpect(jsonPath("$.user.plan").value("FREE"))
                .andExpect(jsonPath("$.user.id").isNumber())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = json.readTree(body);
        String token = node.get("token").asText();

        assertThat(token).hasSizeGreaterThanOrEqualTo(40);
        assertThat(node.get("expiresAt").asText()).endsWith("Z");
        assertThat(apiTokens.findAll()).hasSize(1)
                .allSatisfy(t -> assertThat(t.getTokenHash()).hasSize(64).isNotEqualTo(token));
        assertThat(authEvents.findAll())
                .anyMatch(e -> e.getEventType() == AuthEventType.LOGIN_SUCCESS && EMAIL.equals(e.getEmail()));
    }

    @Test
    void wrongPasswordIs401AndRecordsFailure() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\",\"password\":\"nope-nope\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("invalid_credentials"));

        assertThat(authEvents.findAll())
                .anyMatch(e -> e.getEventType() == AuthEventType.LOGIN_FAILURE && EMAIL.equals(e.getEmail()));
        assertThat(apiTokens.findAll()).isEmpty();
    }

    @Test
    void unknownEmailLooksTheSameAsWrongPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody-api@example.com\",\"password\":\"whatever1\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("invalid_credentials"));
    }

    @Test
    void repeatedFailuresLockOutEvenTheCorrectPassword() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + EMAIL + "\",\"password\":\"bad-pass-" + i + "\"}"))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("too_many_attempts"));
    }

    @Test
    void blankCredentialsAre400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("validation_failed"));
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("bad_request"));
    }

    @Test
    void protectedEndpointsNeedAToken() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("unauthorized"));
        mockMvc.perform(get("/api/v1/conversations").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("unauthorized"));
    }

    @Test
    void meReturnsTheTokenOwner() throws Exception {
        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.plan").value("FREE"));
    }

    @Test
    void logoutRevokesTheToken() throws Exception {
        String token = login();
        mockMvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listsConversationsWithCountsAndPaging() throws Exception {
        mockMvc.perform(get("/api/v1/conversations").header("Authorization", "Bearer " + login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].key").value(conversationKey))
                .andExpect(jsonPath("$.items[0].title").value("Ada Lovelace"))
                .andExpect(jsonPath("$.items[0].initials").value("AL"))
                .andExpect(jsonPath("$.items[0].group").value(false))
                .andExpect(jsonPath("$.items[0].memberCount").value(1))
                .andExpect(jsonPath("$.items[0].preview").value("First message"))
                .andExpect(jsonPath("$.items[0].lastActivityAt").value("2026-01-01T09:00:00Z"))
                .andExpect(jsonPath("$.items[0].unread").value(true))
                .andExpect(jsonPath("$.items[0].threadCount").value(1))
                .andExpect(jsonPath("$.items[0].accounts").isArray())
                .andExpect(jsonPath("$.items[0].senderEmail").value("ada-api@acme.com"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(30))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.counts.all").value(1))
                .andExpect(jsonPath("$.counts.unread").value(1))
                .andExpect(jsonPath("$.counts.awaiting").isNumber())
                .andExpect(jsonPath("$.counts.attachments").value(1));
    }

    @Test
    void listHonoursFilterAndRejectsUnknownOnes() throws Exception {
        String token = login();
        mockMvc.perform(post("/api/v1/conversations/{key}/read", conversationKey)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/conversations").param("filter", "unread")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.counts.unread").value(0))
                .andExpect(jsonPath("$.counts.all").value(1));
        mockMvc.perform(get("/api/v1/conversations").param("filter", "bogus")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("bad_request"));
    }

    @Test
    void detailReturnsTimelineWithoutMarkingRead() throws Exception {
        String token = login();
        mockMvc.perform(get("/api/v1/conversations/{key}", conversationKey)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value(conversationKey))
                .andExpect(jsonPath("$.title").value("Ada Lovelace"))
                .andExpect(jsonPath("$.group").value(false))
                .andExpect(jsonPath("$.threadCount").value(1))
                .andExpect(jsonPath("$.messageCount").value(1))
                .andExpect(jsonPath("$.members[0].email").value("ada-api@acme.com"))
                .andExpect(jsonPath("$.members[0].signatureHtml").isString())
                .andExpect(jsonPath("$.runs[0].outbound").value(false))
                .andExpect(jsonPath("$.runs[0].sender.email").value("ada-api@acme.com"))
                .andExpect(jsonPath("$.runs[0].thread.subject").value("Project Athena"))
                .andExpect(jsonPath("$.runs[0].thread.id").isNumber())
                .andExpect(jsonPath("$.runs[0].messages[0].id").isNumber())
                .andExpect(jsonPath("$.runs[0].messages[0].bodyHtml").isString())
                .andExpect(jsonPath("$.runs[0].messages[0].sentAt").value("2026-01-01T09:00:00Z"))
                .andExpect(jsonPath("$.runs[0].messages[0].outbound").value(false))
                .andExpect(jsonPath("$.runs[0].messages[0].attachments[0].filename").value("spec.pdf"))
                .andExpect(jsonPath("$.runs[0].messages[0].attachments[0].mimeType").value("application/pdf"))
                .andExpect(jsonPath("$.runs[0].messages[0].attachments[0].sizeBytes").value(1234));

        mockMvc.perform(get("/api/v1/conversations").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.items[0].unread").value(true));
    }

    @Test
    void unknownConversationIs404AndOtherUsersCannotSeeIt() throws Exception {
        mockMvc.perform(get("/api/v1/conversations/{key}", "does-not-exist")
                        .header("Authorization", "Bearer " + login()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("not_found"));

        userService.register("api-other@example.com", PASSWORD, null);
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"api-other@example.com\",\"password\":\"" + PASSWORD + "\"}"))
                .andReturn().getResponse().getContentAsString();
        String otherToken = json.readTree(body).get("token").asText();
        mockMvc.perform(get("/api/v1/conversations/{key}", conversationKey)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void websiteStillUsesItsOwnSessionChain() throws Exception {
        mockMvc.perform(get("/chats").header("Authorization", "Bearer " + login()))
                .andExpect(status().is3xxRedirection());
    }
}
