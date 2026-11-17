package com.emailmessenger.api;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.CredentialEncryptor;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** docs/API.md §5.10–5.14. IMAP is mocked at {@link ImapClient}; everything else is real. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiMailboxIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserService userService;
    @Autowired UserRepository users;
    @Autowired MailAccountRepository mailAccounts;
    @Autowired CredentialEncryptor encryptor;
    @PersistenceContext EntityManager em;
    @MockitoBean ImapClient imapClient;

    private User owner;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        owner = userService.register("ada@example.com", "correct-horse", "Ada Lovelace");
        String json = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ApiDtos.LoginRequest("ada@example.com", "correct-horse", null))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readTree(json).get("token").asText();
        when(imapClient.fetchRecentInbox(any(), anyInt())).thenReturn(List.of());
        when(imapClient.fetchRecentSent(any(), anyInt())).thenReturn(List.of());
        when(imapClient.fetchSinceUid(any(), any())).thenReturn(new ImapClient.IncrementalFetch(List.of(), 5L));
        when(imapClient.fetchSentSinceUid(any(), any())).thenReturn(new ImapClient.IncrementalFetch(List.of(), 3L));
    }

    @Test
    void listsMailboxesOldestFirstWithDefaultAndCapacity() throws Exception {
        MailAccount work = mailbox("work@acme.com");
        MailAccount home = mailbox("ada@gmail.com");
        owner.setDefaultMailAccount(home);
        users.save(owner);
        work.markSyncError("AUTHENTICATIONFAILED Invalid credentials");
        mailAccounts.save(work);

        authed(get("/api/v1/mailboxes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canConnect").value(true))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].id").value(work.getId()))
                .andExpect(jsonPath("$.items[0].username").value("work@acme.com"))
                .andExpect(jsonPath("$.items[0].host").value("imap.example.com"))
                .andExpect(jsonPath("$.items[0].port").value(993))
                .andExpect(jsonPath("$.items[0].ssl").value(true))
                .andExpect(jsonPath("$.items[0].authType").value("password"))
                .andExpect(jsonPath("$.items[0].isDefault").value(false))
                .andExpect(jsonPath("$.items[0].lastSyncedAt").doesNotExist())
                .andExpect(jsonPath("$.items[0].syncError").value("AUTHENTICATIONFAILED Invalid credentials"))
                .andExpect(jsonPath("$.items[0].syncErrorHint").value(
                        "Sign-in was rejected. Generate a new app password and reconnect the mailbox."))
                .andExpect(jsonPath("$.items[0].pollingSuspended").value(false))
                .andExpect(jsonPath("$.items[0].createdAt").isString())
                .andExpect(jsonPath("$.items[1].isDefault").value(true))
                .andExpect(jsonPath("$.items[1].syncError").doesNotExist());
    }

    @Test
    void canConnectIsFalseAtThePlanCap() throws Exception {
        mailbox("a@acme.com");
        mailbox("b@acme.com");
        mailbox("c@acme.com");

        authed(get("/api/v1/mailboxes")).andExpect(jsonPath("$.canConnect").value(false));
    }

    @Test
    void providersMirrorTheWebsiteWizard() throws Exception {
        authed(get("/api/v1/mailboxes/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].slug").value("gmail"))
                .andExpect(jsonPath("$.items[0].name").value("Gmail"))
                .andExpect(jsonPath("$.items[0].host").value("imap.gmail.com"))
                .andExpect(jsonPath("$.items[0].port").value(993))
                .andExpect(jsonPath("$.items[0].ssl").value(true))
                .andExpect(jsonPath("$.items[0].appPasswordUrl").value("https://myaccount.google.com/apppasswords"))
                .andExpect(jsonPath("$.items[0].help").isString())
                .andExpect(jsonPath("$.items[4].slug").value("other"))
                .andExpect(jsonPath("$.items[4].host").value(""))
                .andExpect(jsonPath("$.items[4].appPasswordUrl").doesNotExist());
    }

    @Test
    void connectVerifiesStoresAndReturnsTheMailbox() throws Exception {
        String json = connect(Map.of("host", " imap.gmail.com ", "username", " ada@gmail.com ",
                "password", "abcd efgh"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("ada@gmail.com"))
                .andExpect(jsonPath("$.host").value("imap.gmail.com"))
                .andExpect(jsonPath("$.port").value(993))
                .andExpect(jsonPath("$.ssl").value(true))
                .andExpect(jsonPath("$.lastSyncedAt").isString())
                .andExpect(jsonPath("$.syncError").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        verify(imapClient).verifyConnection(
                ImapCredentials.password("imap.gmail.com", 993, true, "ada@gmail.com", "abcd efgh"));
        MailAccount saved = mailAccounts.findById(objectMapper.readTree(json).get("id").asLong()).orElseThrow();
        assertThat(saved.getUser().getId()).isEqualTo(owner.getId());
        assertThat(encryptor.decrypt(saved.getPasswordCiphertext())).isEqualTo("abcd efgh");
    }

    @Test
    void connectRejectedByTheServerIs422AndSavesNothing() throws Exception {
        doThrow(new ImapConnectionException("AUTHENTICATIONFAILED", null)).when(imapClient).verifyConnection(any());

        connect(Map.of("host", "imap.gmail.com", "username", "ada@gmail.com", "password", "wrong"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("mailbox_connect_failed"))
                .andExpect(jsonPath("$.error.message").value(
                        "Could not connect to that mailbox: AUTHENTICATIONFAILED"));
        assertThat(mailAccounts.countByUser(owner)).isZero();
    }

    @Test
    void connectAtThePlanCapIs403BeforeContactingTheServer() throws Exception {
        mailbox("a@acme.com");
        mailbox("b@acme.com");
        mailbox("c@acme.com");

        connect(Map.of("host", "imap.gmail.com", "username", "ada@gmail.com", "password", "pw"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("plan_limit_reached"))
                .andExpect(jsonPath("$.error.message").value(
                        "Your Free plan includes 3 mailboxes. Upgrade on the website to connect more."));
        verify(imapClient, never()).verifyConnection(any());
    }

    @Test
    void connectValidatesInput() throws Exception {
        connect(Map.of("username", "ada@gmail.com", "password", "pw"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("validation_failed"));
        connect(Map.of("host", "h", "port", 70000, "username", "ada@gmail.com", "password", "pw"))
                .andExpect(status().isBadRequest());
        connect(Map.of("host", "h", "username", "  ", "password", "pw"))
                .andExpect(status().isBadRequest());
        Map<String, Object> noPassword = new HashMap<>(Map.of("host", "h", "username", "u"));
        noPassword.put("password", " ");
        connect(noPassword).andExpect(status().isBadRequest());
        verify(imapClient, never()).verifyConnection(any());
    }

    @Test
    void syncReportsImportedCountAndTheUpdatedMailbox() throws Exception {
        MailAccount work = mailbox("work@acme.com");
        work.markSyncError("timed out");
        mailAccounts.save(work);

        authed(post("/api/v1/mailboxes/" + work.getId() + "/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(0))
                .andExpect(jsonPath("$.mailbox.id").value(work.getId()))
                .andExpect(jsonPath("$.mailbox.lastSyncedAt").isString())
                .andExpect(jsonPath("$.mailbox.syncError").doesNotExist());
        verify(imapClient).fetchSinceUid(any(), isNull());
    }

    @Test
    void aFailedSyncIsNotAnHttpError() throws Exception {
        MailAccount work = mailbox("work@acme.com");
        when(imapClient.fetchSinceUid(any(), any())).thenThrow(new ImapConnectionException("Connection timed out", null));

        authed(post("/api/v1/mailboxes/" + work.getId() + "/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(0))
                .andExpect(jsonPath("$.mailbox.syncError").value("Connection timed out"))
                .andExpect(jsonPath("$.mailbox.syncErrorHint").value(
                        "The mail server didn't respond in time. Try again in a moment."));
    }

    @Test
    void syncAndDeleteOfAnotherUsersMailboxAre404() throws Exception {
        User eve = userService.register("eve@example.com", "correct-horse", "Eve");
        MailAccount foreign = mailAccounts.save(new MailAccount(eve, "imap.example.com", 993, true,
                "eve@example.com", encryptor.encrypt("pw")));

        authed(post("/api/v1/mailboxes/" + foreign.getId() + "/sync"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("not_found"));
        authed(delete("/api/v1/mailboxes/" + foreign.getId()))
                .andExpect(status().isNotFound());
        authed(delete("/api/v1/mailboxes/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("validation_failed"));
        assertThat(mailAccounts.findById(foreign.getId())).isPresent();
    }

    @Test
    void deleteRemovesTheMailboxAndClearsTheDefault() throws Exception {
        MailAccount work = mailbox("work@acme.com");
        owner.setDefaultMailAccount(work);
        users.save(owner);
        em.flush();
        em.clear();

        authed(delete("/api/v1/mailboxes/" + work.getId())).andExpect(status().isNoContent());

        em.flush();
        em.clear();
        assertThat(mailAccounts.findById(work.getId())).isEmpty();
        authed(get("/api/v1/mailboxes")).andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void requiresABearerToken() throws Exception {
        mockMvc.perform(get("/api/v1/mailboxes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("unauthorized"));
    }

    private MailAccount mailbox(String username) {
        return mailAccounts.save(new MailAccount(owner, "imap.example.com", 993, true, username,
                encryptor.encrypt("pw")));
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
}
