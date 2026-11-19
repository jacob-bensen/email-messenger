package com.emailmessenger.api;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.ImapClient;
import com.emailmessenger.email.ImapConnectionException;
import com.emailmessenger.email.ImapCredentials;
import com.emailmessenger.repository.MailAccountRepository;
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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** docs/API.md §5.10–5.14. IMAP is mocked at {@link ImapClient}; persistence and polling are real. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiMailboxIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserService userService;
    @Autowired UserRepository users;
    @Autowired MailAccountRepository mailAccounts;
    @PersistenceContext EntityManager em;
    @MockitoBean ImapClient imapClient;

    private User owner;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        owner = userService.register("ada@example.com", "correct-horse", "Ada Lovelace");
        token = login("ada@example.com", "correct-horse");
        when(imapClient.fetchRecentInbox(any(), anyInt())).thenReturn(List.of());
        when(imapClient.fetchRecentSent(any(), anyInt())).thenReturn(List.of());
        when(imapClient.fetchSinceUid(any(), any())).thenReturn(new ImapClient.IncrementalFetch(List.of(), 10L));
        when(imapClient.fetchSentSinceUid(any(), any())).thenReturn(new ImapClient.IncrementalFetch(List.of(), 5L));
    }

    @Test
    void listIsEmptyWithThePlanCap() throws Exception {
        authed(get("/api/v1/mailboxes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.limit").value(3));
    }

    @Test
    void listShowsOwnMailboxesOldestFirstWithDefaultAndErrorHint() throws Exception {
        MailAccount first = mailAccounts.save(new MailAccount(owner, "imap.example.com", 993, true,
                "ada@example.com", "ciphertext"));
        MailAccount second = new MailAccount(owner, "imap.work.com", 143, false, "ada@work.com", "ciphertext");
        second.markSyncError("Login failed: authentication rejected");
        second = mailAccounts.save(second);
        owner.setDefaultMailAccount(second);
        users.save(owner);
        User other = userService.register("bob@example.com", "correct-horse", "Bob");
        mailAccounts.save(new MailAccount(other, "imap.example.com", 993, true, "bob@example.com", "x"));

        authed(get("/api/v1/mailboxes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].id").value(first.getId()))
                .andExpect(jsonPath("$.items[0].address").value("ada@example.com"))
                .andExpect(jsonPath("$.items[0].port").value(993))
                .andExpect(jsonPath("$.items[0].ssl").value(true))
                .andExpect(jsonPath("$.items[0].authType").value("password"))
                .andExpect(jsonPath("$.items[0].isDefault").value(false))
                .andExpect(jsonPath("$.items[0].lastSyncedAt").value(nullValue()))
                .andExpect(jsonPath("$.items[0].errorHint").value(nullValue()))
                .andExpect(jsonPath("$.items[1].host").value("imap.work.com"))
                .andExpect(jsonPath("$.items[1].isDefault").value(true))
                .andExpect(jsonPath("$.items[1].lastSyncError").value("Login failed: authentication rejected"))
                .andExpect(jsonPath("$.items[1].errorHint").value(containsString("Sign-in was rejected")))
                .andExpect(jsonPath("$.items[1].pollingSuspended").value(false));
    }

    @Test
    void providersComeFromTheServer() throws Exception {
        authed(get("/api/v1/mailboxes/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value("gmail"))
                .andExpect(jsonPath("$.items[0].host").value("imap.gmail.com"))
                .andExpect(jsonPath("$.items[0].port").value(993))
                .andExpect(jsonPath("$.items[0].appPasswordUrl").value(containsString("google.com")))
                .andExpect(jsonPath("$.items[4].id").value("other"))
                .andExpect(jsonPath("$.items[4].host").value(""))
                .andExpect(jsonPath("$.items[4].appPasswordUrl").value(nullValue()));
    }

    @Test
    void connectVerifiesStoresEncryptedAndReturnsTheMailbox() throws Exception {
        connect(Map.of("host", " imap.gmail.com ", "username", "ada@gmail.com", "password", "app pass word"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.address").value("ada@gmail.com"))
                .andExpect(jsonPath("$.host").value("imap.gmail.com"))
                .andExpect(jsonPath("$.port").value(993))
                .andExpect(jsonPath("$.ssl").value(true))
                .andExpect(jsonPath("$.lastSyncedAt").isString())
                .andExpect(jsonPath("$.lastSyncError").value(nullValue()))
                .andExpect(jsonPath("$.password").doesNotExist());

        org.mockito.Mockito.verify(imapClient).verifyConnection(argThat((ImapCredentials c) ->
                c.host().equals("imap.gmail.com") && c.username().equals("ada@gmail.com")));
        List<MailAccount> stored = mailAccounts.findByUserOrderByCreatedAtAsc(owner);
        assertThat(stored).hasSize(1);
        assertThat(stored.get(0).getPasswordCiphertext()).isNotEqualTo("app pass word");
    }

    @Test
    void connectWithAFailedFirstImportStillConnects() throws Exception {
        when(imapClient.fetchRecentInbox(any(), anyInt()))
                .thenThrow(new ImapConnectionException("Read timed out", null));

        connect(Map.of("host", "imap.example.com", "port", 143, "ssl", false,
                "username", "ada@example.com", "password", "pw"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.port").value(143))
                .andExpect(jsonPath("$.ssl").value(false))
                .andExpect(jsonPath("$.lastSyncError").value("Read timed out"))
                .andExpect(jsonPath("$.errorHint").value(containsString("didn't respond in time")));
    }

    @Test
    void connectRejectedByTheMailServerStoresNothing() throws Exception {
        doThrow(new ImapConnectionException("AUTHENTICATIONFAILED Invalid credentials", null))
                .when(imapClient).verifyConnection(any());

        connect(Map.of("host", "imap.example.com", "username", "ada@example.com", "password", "wrong"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("mailbox_connect_failed"))
                .andExpect(jsonPath("$.error.message").value(containsString("Invalid credentials")));
        assertThat(mailAccounts.findByUserOrderByCreatedAtAsc(owner)).isEmpty();
    }

    @Test
    void connectValidatesWithTheWebsiteFormRules() throws Exception {
        connect(Map.of("host", " ", "username", "", "password", "pw"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("validation_failed"))
                .andExpect(jsonPath("$.error.message").value("IMAP host is required."));
        connect(Map.of("host", "imap.example.com", "port", 70000, "username", "a", "password", "pw"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("Port must be between 1 and 65535."));
        Map<String, Object> noPassword = new HashMap<>(Map.of("host", "imap.example.com", "username", "a"));
        connect(noPassword)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("Password is required."));
        verifyNoInteractions(imapClient);
    }

    @Test
    void connectAtThePlanCapIsRefused() throws Exception {
        for (int i = 0; i < 3; i++) {
            mailAccounts.save(new MailAccount(owner, "imap.example.com", 993, true, "a" + i + "@example.com", "x"));
        }

        connect(Map.of("host", "imap.example.com", "username", "ada@example.com", "password", "pw"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("plan_limit_reached"))
                .andExpect(jsonPath("$.error.message")
                        .value("Your Free plan allows 3 mailboxes. Upgrade on the website to connect more."));
        verifyNoInteractions(imapClient);
    }

    @Test
    void syncPollsNowAndReturnsTheUpdatedMailbox() throws Exception {
        MailAccount account = connected();

        authed(post("/api/v1/mailboxes/" + account.getId() + "/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(0))
                .andExpect(jsonPath("$.mailbox.id").value(account.getId()))
                .andExpect(jsonPath("$.mailbox.lastSyncedAt").isString())
                .andExpect(jsonPath("$.mailbox.lastSyncError").value(nullValue()));
        assertThat(mailAccounts.findById(account.getId()).orElseThrow().getLastSeenUid()).isEqualTo(10L);
    }

    @Test
    void aFailedSyncIsReportedOnTheMailboxNotAsAnError() throws Exception {
        MailAccount account = connected();
        when(imapClient.fetchSinceUid(any(), any()))
                .thenThrow(new ImapConnectionException("Connection refused: imap.example.com", null));

        authed(post("/api/v1/mailboxes/" + account.getId() + "/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(0))
                .andExpect(jsonPath("$.mailbox.lastSyncError").value("Connection refused: imap.example.com"))
                .andExpect(jsonPath("$.mailbox.errorHint").value(containsString("couldn't reach")));
    }

    @Test
    void syncAndRemoveOfAForeignMailboxAre404() throws Exception {
        User other = userService.register("bob@example.com", "correct-horse", "Bob");
        MailAccount foreign = mailAccounts.save(new MailAccount(other, "imap.example.com", 993, true,
                "bob@example.com", "x"));

        authed(post("/api/v1/mailboxes/" + foreign.getId() + "/sync"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("not_found"));
        authed(delete("/api/v1/mailboxes/" + foreign.getId()))
                .andExpect(status().isNotFound());
        assertThat(mailAccounts.findById(foreign.getId())).isPresent();
        verifyNoInteractions(imapClient);
    }

    @Test
    void removeDisconnectsAndClearsTheDefault() throws Exception {
        MailAccount account = connected();
        owner.setDefaultMailAccount(account);
        users.save(owner);
        em.flush();

        authed(delete("/api/v1/mailboxes/" + account.getId()))
                .andExpect(status().isNoContent());
        em.flush();
        em.clear();

        assertThat(mailAccounts.findById(account.getId())).isEmpty();
        assertThat(users.findById(owner.getId()).orElseThrow().getDefaultMailAccount()).isNull();
        authed(delete("/api/v1/mailboxes/" + account.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void everyEndpointNeedsAToken() throws Exception {
        mockMvc.perform(get("/api/v1/mailboxes")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/mailboxes/providers")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/mailboxes/1/sync")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/mailboxes/1")).andExpect(status().isUnauthorized());
    }

    private MailAccount connected() throws Exception {
        String json = connect(Map.of("host", "imap.example.com", "username", "ada@example.com", "password", "pw"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return mailAccounts.findById(objectMapper.readTree(json).get("id").asLong()).orElseThrow();
    }

    private ResultActions connect(Map<String, ?> body) throws Exception {
        return authed(post("/api/v1/mailboxes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions authed(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {
        return mockMvc.perform(request.header("Authorization", "Bearer " + token));
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
