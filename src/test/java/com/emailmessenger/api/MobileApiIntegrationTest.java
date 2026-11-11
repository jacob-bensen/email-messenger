package com.emailmessenger.api;

import com.emailmessenger.auth.AccountService;
import com.emailmessenger.auth.LoginThrottleService;
import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.AuthEventType;
import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.Message;
import com.emailmessenger.domain.Participant;
import com.emailmessenger.domain.RecipientType;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.MailboxPollingService;
import com.emailmessenger.email.OwnerAddressService;
import com.emailmessenger.repository.ApiTokenRepository;
import com.emailmessenger.repository.AuthEventRepository;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.repository.MessageRepository;
import com.emailmessenger.repository.ParticipantRepository;
import com.emailmessenger.repository.UserRepository;
import com.emailmessenger.service.ConversationKeyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pins the contract in docs/API.md end-to-end: the bearer-token security
 * chain, the login/logout/me endpoints, and the conversations list.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MobileApiIntegrationTest {

    private static final String EMAIL = "mobile@example.com";
    private static final String PASSWORD = "password1";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper json;
    @Autowired UserService userService;
    @Autowired UserRepository users;
    @Autowired AccountService accountService;
    @Autowired LoginThrottleService throttle;
    @Autowired AuthEventRepository authEvents;
    @Autowired ApiTokenRepository apiTokens;
    @Autowired ConversationKeyService keyService;
    @Autowired OwnerAddressService ownerAddressService;
    @Autowired EmailThreadRepository threads;
    @Autowired MessageRepository messages;
    @Autowired ParticipantRepository participants;

    @MockitoBean MailboxPollingService mailboxPollingService;
    @MockitoBean JavaMailSender mailSender;

    private User owner;

    @BeforeEach
    void setUp() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        owner = userService.register(EMAIL, PASSWORD, "Mo Bile");
    }

    @Test
    void loginReturnsTokenExpiryAndUser() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", "  Mobile@Example.com ", "password", PASSWORD,
                                "deviceName", "Pixel 9"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())))
                .andExpect(jsonPath("$.expiresAt", not(emptyOrNullString())))
                .andExpect(jsonPath("$.user.id").value(owner.getId()))
                .andExpect(jsonPath("$.user.email").value(EMAIL))
                .andExpect(jsonPath("$.user.displayName").value("Mo Bile"))
                .andExpect(jsonPath("$.user.emailVerified").value(false));

        assertThat(apiTokens.findAll()).singleElement().satisfies(t -> {
            assertThat(t.getDeviceName()).isEqualTo("Pixel 9");
            assertThat(t.getTokenHash()).hasSize(64);
        });
        assertThat(authEvents.findAll())
                .anyMatch(e -> e.getEventType() == AuthEventType.LOGIN_SUCCESS && EMAIL.equals(e.getEmail()));
    }

    @Test
    void storedHashIsNotThePlaintextToken() throws Exception {
        String token = login();
        assertThat(apiTokens.findAll()).noneMatch(t -> t.getTokenHash().equals(token));
    }

    @Test
    void wrongPasswordAndUnknownEmailAreTheSame401() throws Exception {
        for (String email : List.of(EMAIL, "nobody@example.com")) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(Map.of("email", email, "password", "wrong-password"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("invalid_credentials"))
                    .andExpect(jsonPath("$.error.message").value("Incorrect email or password."));
        }
        assertThat(apiTokens.count()).isZero();
    }

    @Test
    void disabledUserCannotLogInOrKeepUsingAToken() throws Exception {
        String token = login();
        owner.setEnabled(false);
        users.saveAndFlush(owner);

        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", EMAIL, "password", PASSWORD))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("invalid_credentials"));
    }

    @Test
    void blankFieldsAre400ValidationFailed() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", "", "password", PASSWORD))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("validation_failed"));
    }

    @Test
    void unreadableBodyIs400MalformedRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("malformed_request"));
    }

    @Test
    void repeatedFailuresLockTheAccountWith429EvenForTheRightPassword() throws Exception {
        for (int i = 0; i < throttle.getMaxFailures(); i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(Map.of("email", EMAIL, "password", "wrong-password"))))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", EMAIL, "password", PASSWORD))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("too_many_attempts"));
    }

    @Test
    void protectedEndpointsAnswerJson401WithoutAValidToken() throws Exception {
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
        String token = login();
        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(owner.getId()))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.displayName").value("Mo Bile"))
                .andExpect(jsonPath("$.emailVerified").value(false));
    }

    @Test
    void logoutRevokesOnlyThePresentedToken() throws Exception {
        String first = login();
        String second = login();

        mockMvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + first))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + first))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + second))
                .andExpect(status().isOk());
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        String token = login();
        apiTokens.findAll().forEach(t -> {
            org.springframework.test.util.ReflectionTestUtils.setField(
                    t, "expiresAt", LocalDateTime.of(2000, 1, 1, 0, 0));
            apiTokens.saveAndFlush(t);
        });
        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changingThePasswordSignsOutEveryDevice() throws Exception {
        String token = login();
        assertThat(accountService.changePassword(users.findById(owner.getId()).orElseThrow(),
                PASSWORD, "a-new-password")).isEqualTo(AccountService.PasswordChangeOutcome.OK);

        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void conversationsListMatchesTheContract() throws Exception {
        Participant me = participants.save(new Participant(EMAIL, "Mo Bile"));
        Participant ada = participants.save(new Participant("ada@acme.com", "Ada Lovelace"));
        Participant bob = participants.save(new Participant("bob@acme.com", "Bob"));
        inbound("Invoice", ada, List.of(me), "About the invoice", false, LocalDateTime.of(2026, 1, 1, 9, 0));
        inbound("Project", ada, List.of(me, bob), "Kickoff Monday", true, LocalDateTime.of(2026, 1, 3, 9, 30));
        String token = login();

        mockMvc.perform(get("/api/v1/conversations").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].key", not(emptyOrNullString())))
                .andExpect(jsonPath("$.items[0].title").value("Ada Lovelace, Bob"))
                .andExpect(jsonPath("$.items[0].initials").value("AL"))
                .andExpect(jsonPath("$.items[0].group").value(true))
                .andExpect(jsonPath("$.items[0].memberCount").value(2))
                .andExpect(jsonPath("$.items[0].preview").value("Kickoff Monday"))
                .andExpect(jsonPath("$.items[0].lastActivityAt").value("2026-01-03T09:30:00Z"))
                .andExpect(jsonPath("$.items[0].unread").value(true))
                .andExpect(jsonPath("$.items[0].threadCount").value(1))
                .andExpect(jsonPath("$.items[0].accounts[0]").value(EMAIL))
                .andExpect(jsonPath("$.items[0].senderEmail").value("ada@acme.com"))
                .andExpect(jsonPath("$.items[1].title").value("Ada Lovelace"))
                .andExpect(jsonPath("$.items[1].group").value(false))
                .andExpect(jsonPath("$.items[1].unread").value(false))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(30))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.counts.all").value(2))
                .andExpect(jsonPath("$.counts.unread").value(1))
                .andExpect(jsonPath("$.counts.awaiting").isNumber())
                .andExpect(jsonPath("$.counts.attachments").value(0));
        verify(mailboxPollingService).refreshDueForUserAsync(owner.getId());

        mockMvc.perform(get("/api/v1/conversations?filter=unread&size=1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].group").value(true))
                .andExpect(jsonPath("$.size").value(1))
                // counts describe the whole scope, not the filtered page
                .andExpect(jsonPath("$.counts.all").value(2));

        mockMvc.perform(get("/api/v1/conversations?q=invoice").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].title").value("Ada Lovelace"));
    }

    @Test
    void conversationsListOnlyShowsTheCallersOwnMail() throws Exception {
        User other = userService.register("other@example.com", PASSWORD, "Other");
        Participant otherMe = participants.save(new Participant("other@example.com", "Other"));
        Participant ada = participants.save(new Participant("ada@acme.com", "Ada"));
        EmailThread thread = threads.save(new EmailThread(other, "Private", "<private@x>"));
        Message m = new Message(thread, ada, "Private", "not yours", null, LocalDateTime.of(2026, 1, 1, 9, 0));
        m.addRecipient(otherMe, RecipientType.TO);
        thread.addMessage(messages.save(m), true);
        thread.setConversationKey(keyService.compute(thread, ownerAddressService.addressesFor(other)));
        threads.saveAndFlush(thread);

        mockMvc.perform(get("/api/v1/conversations").header("Authorization", "Bearer " + login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.counts.all").value(0));
    }

    @Test
    void conversationsListRejectsBadPagingAndFilter() throws Exception {
        String token = login();
        for (String query : List.of("page=-1", "size=0", "size=51", "filter=bogus", "page=abc")) {
            mockMvc.perform(get("/api/v1/conversations?" + query).header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").exists());
        }
    }

    private String login() throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", EMAIL, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("token").asText();
    }

    private String body(Map<String, String> fields) throws Exception {
        return json.writeValueAsString(fields);
    }

    private void inbound(String subject, Participant sender, List<Participant> to, String text,
                         boolean unread, LocalDateTime at) {
        EmailThread thread = threads.save(new EmailThread(owner, subject, "<" + subject + "@x>"));
        Message m = new Message(thread, sender, subject, text, null, at);
        for (Participant p : to) {
            m.addRecipient(p, RecipientType.TO);
        }
        thread.addMessage(messages.save(m), unread);
        if (!unread) {
            thread.markRead();
        }
        thread.setConversationKey(keyService.compute(thread, ownerAddressService.addressesFor(owner)));
        threads.saveAndFlush(thread);
    }
}
