package com.emailmessenger.web;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.GmailOAuthClient;
import com.emailmessenger.email.ImapConnectionException;
import com.emailmessenger.email.MailAccountService;
import com.emailmessenger.repository.UserRepository;
import com.emailmessenger.service.ReplyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "auth.google.client-id=test-client-id",
        "auth.google.client-secret=test-client-secret"
})
@Transactional
class GmailConnectControllerTest {

    private static final String SESSION_STATE = "conexusmail.gmail.oauth.state";

    @Autowired MockMvc mockMvc;
    @Autowired UserService userService;
    @Autowired UserRepository userRepository;

    @MockitoBean GmailOAuthClient gmailOAuthClient;
    @MockitoBean MailAccountService mailAccountService;
    @MockitoBean ReplyService replyService;

    @BeforeEach
    void setUp() {
        userService.register("gmail-user@example.com", "password1", "Gmail User");
        userRepository.findByEmail("gmail-user@example.com").orElseThrow();
    }

    @Test
    void connectRedirectsToGoogleAuthorizationUrl() throws Exception {
        when(gmailOAuthClient.buildAuthorizationUrl(anyString(), anyString()))
                .thenReturn("https://accounts.google.com/o/oauth2/v2/auth?stub=1");

        mockMvc.perform(get("/mailboxes/gmail/connect").with(user("gmail-user@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://accounts.google.com/o/oauth2/v2/auth?stub=1"));
    }

    @Test
    void callbackWithMismatchedStateRedirectsWithError() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SESSION_STATE, "expected-state");

        mockMvc.perform(get("/mailboxes/gmail/callback")
                        .param("code", "auth-code")
                        .param("state", "forged-state")
                        .session(session)
                        .with(user("gmail-user@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mailboxes/new?provider=gmail"))
                .andExpect(flash().attributeExists("connectError"));

        verify(mailAccountService, never())
                .connectGmailOAuth(any(User.class), anyString(), anyString(), anyString());
    }

    @Test
    void callbackHappyPathConnectsMailboxAndRedirectsToThreads() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SESSION_STATE, "good-state");

        when(gmailOAuthClient.exchangeCode(eq("auth-code"), anyString()))
                .thenReturn(new GmailOAuthClient.TokenResult("access-tok", "refresh-tok"));
        when(gmailOAuthClient.fetchEmail("access-tok")).thenReturn("connected@gmail.com");
        MailAccount account = Mockito.mock(MailAccount.class);
        when(account.getLastSyncError()).thenReturn(null);
        when(mailAccountService.connectGmailOAuth(any(User.class), eq("connected@gmail.com"),
                eq("refresh-tok"), eq("access-tok"))).thenReturn(account);

        mockMvc.perform(get("/mailboxes/gmail/callback")
                        .param("code", "auth-code")
                        .param("state", "good-state")
                        .session(session)
                        .with(user("gmail-user@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/threads"));

        verify(mailAccountService).connectGmailOAuth(any(User.class), eq("connected@gmail.com"),
                eq("refresh-tok"), eq("access-tok"));
    }

    @Test
    void callbackWithMissingRefreshTokenRedirectsWithError() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SESSION_STATE, "good-state");

        when(gmailOAuthClient.exchangeCode(eq("auth-code"), anyString()))
                .thenReturn(new GmailOAuthClient.TokenResult("access-tok", null));

        mockMvc.perform(get("/mailboxes/gmail/callback")
                        .param("code", "auth-code")
                        .param("state", "good-state")
                        .session(session)
                        .with(user("gmail-user@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mailboxes/new?provider=gmail"))
                .andExpect(flash().attributeExists("connectError"));

        verify(mailAccountService, never())
                .connectGmailOAuth(any(User.class), anyString(), anyString(), anyString());
    }

    @Test
    void callbackSurfacesImapFailureAsError() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SESSION_STATE, "good-state");

        when(gmailOAuthClient.exchangeCode(eq("auth-code"), anyString()))
                .thenReturn(new GmailOAuthClient.TokenResult("access-tok", "refresh-tok"));
        when(gmailOAuthClient.fetchEmail("access-tok")).thenReturn("connected@gmail.com");
        when(mailAccountService.connectGmailOAuth(any(User.class), anyString(), anyString(), anyString()))
                .thenThrow(new ImapConnectionException("AUTHENTICATE failed", null));

        mockMvc.perform(get("/mailboxes/gmail/callback")
                        .param("code", "auth-code")
                        .param("state", "good-state")
                        .session(session)
                        .with(user("gmail-user@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mailboxes/new?provider=gmail"))
                .andExpect(flash().attributeExists("connectError"));
    }
}
