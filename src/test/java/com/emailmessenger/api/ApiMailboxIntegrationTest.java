package com.emailmessenger.api;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.AuthType;
import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.ImapClient;
import com.emailmessenger.email.ImapConnectionException;
import com.emailmessenger.email.ImapCredentials;
import com.emailmessenger.repository.MailAccountRepository;
import com.emailmessenger.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** docs/API.md §5.10–5.14. IMAP is mocked at {@link ImapClient}; services and persistence are real. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiMailboxIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserService userService;
    @Autowired UserRepository users;
    @Autowired MailAccountRepository mailAccounts;
    @MockitoBean ImapClient imapClient;

    private User owner;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        owner = userService.register("ada@example.com", "correct-horse", "Ada Lovelace");
        token = login("ada@example.com", "correct-horse");
        when(imapClient.fetchRecentInbox(any(), anyInt())).thenReturn(List.of());
        when(imapClient.fetchRecentSent(any(), anyInt())).thenReturn(List.of());
        when(imapClient.fetchSinceUid(any(), any())).thenReturn(new ImapClient.IncrementalFetch(List.of(), 41L));
        when(imapClient.fetchSentSinceUid(any(), any())).thenReturn(new ImapClient.IncrementalFetch(List.of(), 7L));
    }

    @Test
    void listsOwnMailboxesWithDefaultSenderAndHint() throws Exception {
        MailAccount home = mailAccounts.save(new MailAccount(owner, "imap.example.com", 993, true, "ada@example.com", "c"));
        MailAccount work = new MailAccount(owner, "imap.acme.com", 143, false, "work@acme.com", "c");
        work.markSyncError("AUTHENTICATE failed");
        work = mailAccounts.save(work);
        owner.setDefaultMailAccount(work);
        users.save(owner);
        User other = userService.register("eve@example.com", "password1", null);
        mailAccounts.save(new MailAccount(other, "imap.evil.com", 993, true, "eve@example.com", "c"));

        mockMvc.perform(get("/api/v1/mailboxes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].id").value(home.getId()))
                .andExpect(jsonPath("$.items[0].authType").value("password"))
                .andExpect(jsonPath("$.items[0].lastSyncedAt").isEmpty())
                .andExpect(jsonPath("$.items[0].errorHint").isEmpty())
                .andExpect(jsonPath("$.items[0].defaultSender").value(false))
                .andExpect(jsonPath("$.items[1].port").value(143))
                .andExpect(jsonPath("$.items[1].ssl").value(false))
                .andExpect(jsonPath("$.items[1].lastSyncError").value("AUTHENTICATE failed"))
                .andExpect(jsonPath("$.items[1].errorHint").value(org.hamcrest.Matchers.containsString("app password")))
                .andExpect(jsonPath("$.items[1].defaultSender").value(true));
    }

    @Test
    void googleMailboxesReportTheirAuthType() throws Exception {
        mailAccounts.save(new MailAccount(owner, "imap.gmail.com", 993, true, "ada@gmail.com", "c", AuthType.XOAUTH2));

        mockMvc.perform(get("/api/v1/mailboxes").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.items[0].authType").value("google"));
    }

    @Test
    void providersMatchTheWebsiteWizard() throws Exception {
        mockMvc.perform(get("/api/v1/mailboxes/providers").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].slug").value("gmail"))
                .andExpect(jsonPath("$.items[0].host").value("imap.gmail.com"))
                .andExpect(jsonPath("$.items[0].port").value(993))
                .andExpect(jsonPath("$.items[0].appPasswordUrl").isNotEmpty())
                .andExpect(jsonPath("$.items[4].slug").value("other"))
                .andExpect(jsonPath("$.items[4].host").value(""))
                .andExpect(jsonPath("$.items[4].appPasswordUrl").isEmpty());
    }

    @Test
    void connectVerifiesSavesAndReturnsTheMailbox() throws Exception {
        connect(Map.of("host", " imap.gmail.com ", "username", " ada@gmail.com ", "password", "app pass"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.host").value("imap.gmail.com"))
                .andExpect(jsonPath("$.port").value(993))
                .andExpect(jsonPath("$.ssl").value(true))
                .andExpect(jsonPath("$.username").value("ada@gmail.com"))
                .andExpect(jsonPath("$.lastSyncedAt").isNotEmpty())
                .andExpect(jsonPath("$.lastSyncError").isEmpty());

        List<MailAccount> saved = mailAccounts.findByUserOrderByCreatedAtAsc(owner);
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getPasswordCiphertext()).isNotEqualTo("app pass");
    }

    @Test
    void connectRejectedByTheServerSavesNothing() throws Exception {
        doThrow(new ImapConnectionException("AUTHENTICATE failed", null))
                .when(imapClient).verifyConnection(any(ImapCredentials.class));

        connect(Map.of("host", "imap.gmail.com", "username", "ada@gmail.com", "password", "wrong"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("mailbox_connect_failed"))
                .andExpect(jsonPath("$.error.message").value("Could not connect to that mailbox: AUTHENTICATE failed"));
        assertThat(mailAccounts.findByUserOrderByCreatedAtAsc(owner)).isEmpty();
    }

    @Test
    void connectBeyondThePlanCapExplainsTheLimit() throws Exception {
        for (int i = 0; i < 3; i++) {
            mailAccounts.save(new MailAccount(owner, "imap.example.com", 993, true, "a" + i + "@example.com", "c"));
        }

        connect(Map.of("host", "imap.gmail.com", "username", "ada@gmail.com", "password", "pw"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("plan_limit_reached"))
                .andExpect(jsonPath("$.error.message")
                        .value("Your Free plan includes 3 mailboxes. Upgrade on the website to connect more."));
        verifyNoInteractions(imapClient);
    }

    @Test
    void connectValidatesInput() throws Exception {
        Map<String, Object> body = new HashMap<>(Map.of("host", "imap.gmail.com", "username", "ada", "password", "pw"));
        body.put("port", 0);
        connect(body).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("validation_failed"));
        connect(Map.of("host", "  ", "username", "ada", "password", "pw")).andExpect(status().isBadRequest());
        connect(Map.of("host", "imap.gmail.com", "username", "ada", "password", " ")).andExpect(status().isBadRequest());
        connect(Map.of("host", "imap.gmail.com", "password", "pw")).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/mailboxes").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(imapClient);
    }

    @Test
    void syncPollsNowAndReturnsTheResult() throws Exception {
        MailAccount account = new MailAccount(owner, "imap.example.com", 993, true, "ada@example.com", "c");
        account.markSyncError("Read timed out");
        account = mailAccounts.save(account);
        // "c" is not real ciphertext, so polling `account` fails at credential resolution;
        // a successful poll needs a mailbox connected through the API.
        connect(Map.of("host", "imap.other.com", "username", "ada@other.com", "password", "pw"))
                .andExpect(status().isCreated());
        MailAccount connected = mailAccounts.findByUserOrderByCreatedAtAsc(owner).get(1);

        mockMvc.perform(post("/api/v1/mailboxes/" + connected.getId() + "/sync")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(connected.getId()))
                .andExpect(jsonPath("$.lastSyncError").isEmpty());
        assertThat(mailAccounts.findById(connected.getId()).orElseThrow().getLastSeenUid()).isEqualTo(41L);

        mockMvc.perform(post("/api/v1/mailboxes/" + account.getId() + "/sync")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastSyncError").isNotEmpty())
                .andExpect(jsonPath("$.errorHint").isNotEmpty());
    }

    @Test
    void syncAndDeleteOfAnotherUsersMailboxAre404() throws Exception {
        User other = userService.register("eve@example.com", "password1", null);
        MailAccount foreign = mailAccounts.save(new MailAccount(other, "imap.evil.com", 993, true, "eve@example.com", "c"));

        mockMvc.perform(post("/api/v1/mailboxes/" + foreign.getId() + "/sync").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("not_found"));
        mockMvc.perform(delete("/api/v1/mailboxes/" + foreign.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        assertThat(mailAccounts.findById(foreign.getId())).isPresent();
    }

    @Test
    void deleteRemovesTheMailbox() throws Exception {
        MailAccount account = mailAccounts.save(new MailAccount(owner, "imap.example.com", 993, true, "ada@example.com", "c"));

        mockMvc.perform(delete("/api/v1/mailboxes/" + account.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        assertThat(mailAccounts.findById(account.getId())).isEmpty();
    }

    @Test
    void requiresABearerToken() throws Exception {
        mockMvc.perform(get("/api/v1/mailboxes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("unauthorized"));
    }

    private ResultActions connect(Map<String, ?> body) throws Exception {
        return mockMvc.perform(post("/api/v1/mailboxes")
                .header("Authorization", "Bearer " + token)
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
