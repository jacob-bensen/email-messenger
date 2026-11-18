package com.emailmessenger.api;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.Subscription;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.ImapClient;
import com.emailmessenger.email.ImapConnectionException;
import com.emailmessenger.email.ImapCredentials;
import com.emailmessenger.repository.MailAccountRepository;
import com.emailmessenger.repository.SubscriptionRepository;
import com.emailmessenger.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** docs/API.md §5.10–5.15. IMAP is mocked at {@link ImapClient}; persistence and encryption are real. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiMailboxIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserService userService;
    @Autowired UserRepository users;
    @Autowired MailAccountRepository mailAccounts;
    @Autowired SubscriptionRepository subscriptions;
    @PersistenceContext EntityManager em;
    @MockitoBean ImapClient imapClient;

    private User owner;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        owner = userService.register("ada@example.com", "correct-horse", "Ada Lovelace");
        token = signIn("ada@example.com", "correct-horse");
        when(imapClient.fetchRecentInbox(any(ImapCredentials.class), anyInt())).thenReturn(List.of());
        when(imapClient.fetchSinceUid(any(ImapCredentials.class), any()))
                .thenReturn(new ImapClient.IncrementalFetch(List.of(), 42L));
        when(imapClient.fetchRecentSent(any(ImapCredentials.class), anyInt())).thenReturn(List.of());
        when(imapClient.fetchSentSinceUid(any(ImapCredentials.class), any()))
                .thenReturn(new ImapClient.IncrementalFetch(List.of(), null));
    }

    @Test
    void listIsEmptyWithTheFreePlanCap() throws Exception {
        mockMvc.perform(get("/api/v1/mailboxes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.defaultMailboxId").value((Object) null))
                .andExpect(jsonPath("$.limit").value(3));
    }

    @Test
    void connectVerifiesStoresEncryptedAndListsTheMailbox() throws Exception {
        String json = connect(Map.of("host", " imap.gmail.com ", "username", " ada@gmail.com ",
                "password", "app pass"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.address").value("ada@gmail.com"))
                .andExpect(jsonPath("$.host").value("imap.gmail.com"))
                .andExpect(jsonPath("$.port").value(993))
                .andExpect(jsonPath("$.ssl").value(true))
                .andExpect(jsonPath("$.auth").value("password"))
                .andExpect(jsonPath("$.lastSyncedAt").isString())
                .andExpect(jsonPath("$.syncError").value((Object) null))
                .andExpect(jsonPath("$.syncErrorHint").value((Object) null))
                .andExpect(jsonPath("$.pollingSuspended").value(false))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(json).get("id").asLong();

        verify(imapClient).verifyConnection(any(ImapCredentials.class));
        MailAccount stored = mailAccounts.findById(id).orElseThrow();
        assertThat(stored.getPasswordCiphertext()).isNotEqualTo("app pass");
        assertThat(json).doesNotContain("app pass");

        owner.setDefaultMailAccount(stored);
        users.save(owner);
        mockMvc.perform(get("/api/v1/mailboxes").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(id))
                .andExpect(jsonPath("$.defaultMailboxId").value(id));
    }

    @Test
    void connectValidatesWithTheWebsiteFormRules() throws Exception {
        connect(Map.of("host", "  ", "username", "ada@gmail.com", "password", "x"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("validation_failed"))
                .andExpect(jsonPath("$.error.message").value("IMAP host is required."));
        connect(Map.of("host", "imap.gmail.com", "port", 70000, "username", "ada@gmail.com", "password", "x"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("Port must be between 1 and 65535."));
        Map<String, Object> noPassword = new HashMap<>();
        noPassword.put("host", "imap.gmail.com");
        noPassword.put("username", "ada@gmail.com");
        noPassword.put("password", null);
        connect(noPassword)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("Password is required."));
        verifyNoInteractions(imapClient);
    }

    @Test
    void rejectedLoginIs422AndSavesNothing() throws Exception {
        doThrow(new ImapConnectionException("AUTHENTICATIONFAILED Invalid credentials", null))
                .when(imapClient).verifyConnection(any(ImapCredentials.class));

        connect(Map.of("host", "imap.gmail.com", "username", "ada@gmail.com", "password", "wrong"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("mailbox_connect_failed"))
                .andExpect(jsonPath("$.error.message")
                        .value("Could not connect to that mailbox: AUTHENTICATIONFAILED Invalid credentials"));
        assertThat(mailAccounts.findByUserOrderByCreatedAtAsc(owner)).isEmpty();
    }

    @Test
    void connectBeyondThePlanCapIs403() throws Exception {
        for (int i = 0; i < 3; i++) {
            mailAccounts.save(new MailAccount(owner, "imap.example.com", 993, true, "box" + i + "@example.com", "c"));
        }
        connect(Map.of("host", "imap.gmail.com", "username", "ada@gmail.com", "password", "pw"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("plan_limit_reached"))
                .andExpect(jsonPath("$.error.message")
                        .value("Your Free plan includes 3 mailboxes. Upgrade on the website to connect more."));
        verifyNoInteractions(imapClient);
    }

    @Test
    void businessPlanListsNoCap() throws Exception {
        Subscription sub = new Subscription(owner, "cus_biz", "active");
        sub.setPlan(Plan.BUSINESS);
        subscriptions.save(sub);
        mockMvc.perform(get("/api/v1/mailboxes").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.limit").value((Object) null));
    }

    @Test
    void syncNowPollsAndReturnsTheReloadedMailbox() throws Exception {
        long id = connectedMailboxId();

        mockMvc.perform(post("/api/v1/mailboxes/" + id + "/sync").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(0))
                .andExpect(jsonPath("$.mailbox.id").value(id))
                .andExpect(jsonPath("$.mailbox.syncError").value((Object) null));
        assertThat(mailAccounts.findById(id).orElseThrow().getLastSeenUid()).isEqualTo(42L);
    }

    @Test
    void failedSyncIsStill200WithTheErrorAndHint() throws Exception {
        long id = connectedMailboxId();
        when(imapClient.fetchSinceUid(any(ImapCredentials.class), any()))
                .thenThrow(new ImapConnectionException("Connection timed out", null));

        mockMvc.perform(post("/api/v1/mailboxes/" + id + "/sync").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(0))
                .andExpect(jsonPath("$.mailbox.syncError").value("Connection timed out"))
                .andExpect(jsonPath("$.mailbox.syncErrorHint")
                        .value("The mail server didn't respond in time. Try again in a moment."));
    }

    @Test
    void deleteRemovesTheMailboxAndIsOwnerScoped() throws Exception {
        long id = connectedMailboxId();
        userService.register("eve@example.com", "correct-horse", "Eve");
        String eve = signIn("eve@example.com", "correct-horse");

        mockMvc.perform(delete("/api/v1/mailboxes/" + id).header("Authorization", "Bearer " + eve))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("not_found"));
        mockMvc.perform(post("/api/v1/mailboxes/" + id + "/sync").header("Authorization", "Bearer " + eve))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/mailboxes/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        assertThat(mailAccounts.findById(id)).isEmpty();
        mockMvc.perform(delete("/api/v1/mailboxes/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/mailboxes/abc").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void providersMirrorTheWebsiteWizard() throws Exception {
        mockMvc.perform(get("/api/v1/mailboxes/providers").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].slug").value("gmail"))
                .andExpect(jsonPath("$.items[0].name").value("Gmail"))
                .andExpect(jsonPath("$.items[0].host").value("imap.gmail.com"))
                .andExpect(jsonPath("$.items[0].port").value(993))
                .andExpect(jsonPath("$.items[0].ssl").value(true))
                .andExpect(jsonPath("$.items[0].appPasswordUrl").value("https://myaccount.google.com/apppasswords"))
                .andExpect(jsonPath("$.items[4].slug").value("other"))
                .andExpect(jsonPath("$.items[4].host").value(""))
                .andExpect(jsonPath("$.items[4].appPasswordUrl").value((Object) null));
    }

    @Test
    void everyEndpointNeedsAToken() throws Exception {
        mockMvc.perform(get("/api/v1/mailboxes")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/mailboxes/providers")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/mailboxes").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/mailboxes/1")).andExpect(status().isUnauthorized());
    }

    private long connectedMailboxId() throws Exception {
        String json = connect(Map.of("host", "imap.gmail.com", "username", "ada@gmail.com", "password", "pw"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        em.flush();
        return objectMapper.readTree(json).get("id").asLong();
    }

    private ResultActions connect(Map<String, ?> body) throws Exception {
        return mockMvc.perform(post("/api/v1/mailboxes")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private String signIn(String email, String password) throws Exception {
        String json = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApiDtos.LoginRequest(email, password, null))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("token").asText();
    }
}
