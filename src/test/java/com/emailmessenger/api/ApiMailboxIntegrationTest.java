package com.emailmessenger.api;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.CredentialEncryptor;
import com.emailmessenger.email.ImapClient;
import com.emailmessenger.email.ImapConnectionException;
import com.emailmessenger.repository.MailAccountRepository;
import com.emailmessenger.repository.UserRepository;
import com.emailmessenger.service.ReplyService;
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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** docs/API.md §5.10–5.14. IMAP is mocked at {@link ImapClient}; persistence and plan rules are real. */
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
    @MockitoBean ImapClient imapClient;
    @MockitoBean ReplyService replyService;

    private User owner;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        owner = userService.register("ada@example.com", "correct-horse", "Ada Lovelace");
        token = signIn("ada@example.com", "correct-horse");
        when(imapClient.fetchRecentInbox(any(), anyInt())).thenReturn(List.of());
        when(imapClient.fetchSinceUid(any(), any())).thenReturn(new ImapClient.IncrementalFetch(List.of(), null));
        when(imapClient.fetchRecentSent(any(), anyInt())).thenReturn(List.of());
        when(imapClient.fetchSentSinceUid(any(), any())).thenReturn(new ImapClient.IncrementalFetch(List.of(), null));
    }

    @Test
    void listIsEmptyWithTheFreePlanCap() throws Exception {
        authed(get("/api/v1/mailboxes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.limit").value(3))
                .andExpect(jsonPath("$.canConnectMore").value(true));
    }

    @Test
    void connectVerifiesImportsAndReturnsTheMailbox() throws Exception {
        connect(Map.of("host", "imap.example.com", "port", 993, "ssl", true,
                "username", "ada@work.com", "password", "app-pw"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.address").value("ada@work.com"))
                .andExpect(jsonPath("$.host").value("imap.example.com"))
                .andExpect(jsonPath("$.authType").value("password"))
                .andExpect(jsonPath("$.defaultSender").value(false))
                .andExpect(jsonPath("$.lastSyncedAt").isString())
                .andExpect(jsonPath("$.lastSyncError").isEmpty())
                .andExpect(jsonPath("$.password").doesNotExist());

        assertThat(mailAccounts.findByUserOrderByCreatedAtAsc(owner)).hasSize(1);
        authed(get("/api/v1/mailboxes"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].address").value("ada@work.com"));
    }

    @Test
    void rejectedCredentialsAre422AndNothingIsSaved() throws Exception {
        doThrow(new ImapConnectionException("AUTHENTICATE failed", null)).when(imapClient).verifyConnection(any());

        connect(Map.of("host", "imap.example.com", "username", "ada@work.com", "password", "wrong"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("mailbox_connect_failed"))
                .andExpect(jsonPath("$.error.message").value(containsString("AUTHENTICATE failed")));

        assertThat(mailAccounts.findByUserOrderByCreatedAtAsc(owner)).isEmpty();
    }

    @Test
    void invalidFieldsAre400WithTheFieldMessage() throws Exception {
        connect(Map.of("host", "imap.example.com", "port", 0, "username", "ada@work.com", "password", "pw"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("validation_failed"))
                .andExpect(jsonPath("$.error.message").value("Port must be between 1 and 65535."));
        verifyNoInteractions(imapClient);
    }

    @Test
    void theFourthMailboxOnFreeIsAPlanLimitError() throws Exception {
        for (int i = 0; i < 3; i++) {
            mailAccounts.save(new MailAccount(owner, "imap.example.com", 993, true, "box" + i + "@x.com", "c"));
        }
        authed(get("/api/v1/mailboxes")).andExpect(jsonPath("$.canConnectMore").value(false));

        connect(Map.of("host", "imap.example.com", "username", "box4@x.com", "password", "pw"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("plan_limit_reached"))
                .andExpect(jsonPath("$.error.message").value(containsString("Free plan connects up to 3")));
        verifyNoInteractions(imapClient);
    }

    @Test
    void syncPollsTheMailboxAndReportsItsState() throws Exception {
        MailAccount box = mailAccounts.save(new MailAccount(owner, "imap.example.com", 993, true,
                "ada@work.com", encryptor.encrypt("app-pw")));

        authed(post("/api/v1/mailboxes/" + box.getId() + "/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(0))
                .andExpect(jsonPath("$.mailbox.id").value(box.getId()))
                .andExpect(jsonPath("$.mailbox.lastSyncedAt").isString())
                .andExpect(jsonPath("$.mailbox.lastSyncError").isEmpty());
    }

    @Test
    void aFailedSyncIsStill200WithTheErrorAndHint() throws Exception {
        MailAccount box = mailAccounts.save(new MailAccount(owner, "imap.example.com", 993, true,
                "ada@work.com", encryptor.encrypt("app-pw")));
        when(imapClient.fetchSinceUid(any(), isNull()))
                .thenThrow(new ImapConnectionException("Connection timed out", null));

        authed(post("/api/v1/mailboxes/" + box.getId() + "/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mailbox.lastSyncError").value(containsString("timed out")))
                .andExpect(jsonPath("$.mailbox.errorHint").value(containsString("didn't respond in time")));
    }

    @Test
    void anotherUsersMailboxIs404ForSyncAndDelete() throws Exception {
        User other = userService.register("bob@example.com", "correct-horse", "Bob");
        MailAccount foreign = mailAccounts.save(new MailAccount(other, "imap.example.com", 993, true,
                "bob@work.com", "c"));

        authed(post("/api/v1/mailboxes/" + foreign.getId() + "/sync"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("not_found"));
        authed(delete("/api/v1/mailboxes/" + foreign.getId()))
                .andExpect(status().isNotFound());
        assertThat(mailAccounts.findById(foreign.getId())).isPresent();
    }

    @Test
    void deleteRemovesTheMailbox() throws Exception {
        MailAccount box = mailAccounts.save(new MailAccount(owner, "imap.example.com", 993, true,
                "ada@work.com", "c"));

        authed(delete("/api/v1/mailboxes/" + box.getId())).andExpect(status().isNoContent());

        assertThat(mailAccounts.findById(box.getId())).isEmpty();
    }

    @Test
    void defaultSenderIsFlagged() throws Exception {
        MailAccount box = mailAccounts.save(new MailAccount(owner, "imap.example.com", 993, true,
                "ada@work.com", "c"));
        owner.setDefaultMailAccount(box);
        users.save(owner);

        authed(get("/api/v1/mailboxes")).andExpect(jsonPath("$.items[0].defaultSender").value(true));
    }

    @Test
    void providersListThePresets() throws Exception {
        authed(get("/api/v1/mailboxes/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("gmail"))
                .andExpect(jsonPath("$[0].host").value("imap.gmail.com"))
                .andExpect(jsonPath("$[0].port").value(993))
                .andExpect(jsonPath("$[?(@.id == 'other')].host").value(org.hamcrest.Matchers.contains((Object) null)));
    }

    @Test
    void everyEndpointNeedsAToken() throws Exception {
        mockMvc.perform(get("/api/v1/mailboxes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("unauthorized"));
    }

    private ResultActions connect(Map<String, Object> body) throws Exception {
        return authed(post("/api/v1/mailboxes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions authed(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {
        return mockMvc.perform(request.header("Authorization", "Bearer " + token));
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
