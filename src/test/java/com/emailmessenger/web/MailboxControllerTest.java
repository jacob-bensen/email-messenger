package com.emailmessenger.web;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.billing.PlanLimitExceededException;
import com.emailmessenger.billing.PlanLimitKind;
import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.ImapConnectionException;
import com.emailmessenger.email.MailAccountService;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.repository.UserRepository;
import com.emailmessenger.service.ReplyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class MailboxControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserService userService;
    @Autowired UserRepository userRepository;

    @MockBean MailAccountService mailAccountService;
    // Avoid SMTP / Stripe wiring noise.
    @MockBean ReplyService replyService;
    @MockBean EmailThreadRepository threadRepository;

    private User owner;

    @BeforeEach
    void setUp() {
        userService.register("mailbox@example.com", "password1", "Mailbox User");
        owner = userRepository.findByEmail("mailbox@example.com").orElseThrow();
        when(mailAccountService.list(any(User.class))).thenReturn(java.util.List.of());
    }

    @Test
    void anonymousIsRedirectedToLoginForGetMailboxes() throws Exception {
        mockMvc.perform(get("/mailboxes"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void authenticatedGetRendersMailboxesIndex() throws Exception {
        mockMvc.perform(get("/mailboxes").with(user("mailbox@example.com")))
                .andExpect(status().isOk())
                .andExpect(view().name("mailboxes/index"))
                .andExpect(model().attributeExists("mailboxes"));
    }

    @Test
    void newMailboxWithoutProviderRendersPicker() throws Exception {
        mockMvc.perform(get("/mailboxes/new").with(user("mailbox@example.com")))
                .andExpect(status().isOk())
                .andExpect(view().name("mailboxes/new"))
                .andExpect(model().attributeExists("providers"))
                .andExpect(model().attributeDoesNotExist("provider"));
    }

    @Test
    void newMailboxWithGmailPresetFillsHostAndPort() throws Exception {
        mockMvc.perform(get("/mailboxes/new")
                        .param("provider", "gmail")
                        .with(user("mailbox@example.com")))
                .andExpect(status().isOk())
                .andExpect(view().name("mailboxes/new"))
                .andExpect(model().attributeExists("mailboxForm"))
                .andExpect(model().attribute("mailboxForm",
                        org.hamcrest.Matchers.hasProperty("host",
                                org.hamcrest.Matchers.equalTo("imap.gmail.com"))))
                .andExpect(model().attribute("mailboxForm",
                        org.hamcrest.Matchers.hasProperty("port",
                                org.hamcrest.Matchers.equalTo(993))))
                .andExpect(model().attribute("mailboxForm",
                        org.hamcrest.Matchers.hasProperty("provider",
                                org.hamcrest.Matchers.equalTo("gmail"))))
                .andExpect(model().attributeExists("provider"));
    }

    @Test
    void newMailboxWithUnknownProviderFallsBackToPicker() throws Exception {
        mockMvc.perform(get("/mailboxes/new")
                        .param("provider", "bogus")
                        .with(user("mailbox@example.com")))
                .andExpect(status().isOk())
                .andExpect(view().name("mailboxes/new"))
                .andExpect(model().attributeDoesNotExist("provider"));
    }

    @Test
    void postWithMissingFieldsRendersFormWithErrors() throws Exception {
        mockMvc.perform(post("/mailboxes")
                        .with(user("mailbox@example.com"))
                        .with(csrf())
                        .param("host", "")
                        .param("port", "993")
                        .param("username", "")
                        .param("password", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("mailboxes/new"))
                .andExpect(model().attributeHasFieldErrors("mailboxForm", "host", "username", "password"));

        verify(mailAccountService, never()).connect(any(User.class), anyString(), anyInt(),
                anyBoolean(), anyString(), anyString());
    }

    @Test
    void successfulConnectRedirectsToThreads() throws Exception {
        MailAccount account = Mockito.mock(MailAccount.class);
        when(account.getLastSyncError()).thenReturn(null);
        when(mailAccountService.connect(any(User.class), eq("imap.example.com"), eq(993), eq(true),
                eq("user@example.com"), eq("app-pw"))).thenReturn(account);

        mockMvc.perform(post("/mailboxes")
                        .with(user("mailbox@example.com"))
                        .with(csrf())
                        .param("host", "imap.example.com")
                        .param("port", "993")
                        .param("ssl", "true")
                        .param("username", "user@example.com")
                        .param("password", "app-pw"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/threads"));
    }

    @Test
    void syncErrorRedirectsToMailboxesIndex() throws Exception {
        MailAccount account = Mockito.mock(MailAccount.class);
        when(account.getLastSyncError()).thenReturn("INBOX unreachable");
        when(mailAccountService.connect(any(User.class), anyString(), anyInt(), anyBoolean(),
                anyString(), anyString())).thenReturn(account);

        mockMvc.perform(post("/mailboxes")
                        .with(user("mailbox@example.com"))
                        .with(csrf())
                        .param("host", "imap.example.com")
                        .param("port", "993")
                        .param("ssl", "true")
                        .param("username", "user@example.com")
                        .param("password", "app-pw"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mailboxes"));
    }

    @Test
    void imapConnectionFailureRendersFormWithError() throws Exception {
        when(mailAccountService.connect(any(User.class), anyString(), anyInt(), anyBoolean(),
                anyString(), anyString()))
                .thenThrow(new ImapConnectionException("AUTHENTICATE failed", null));

        mockMvc.perform(post("/mailboxes")
                        .with(user("mailbox@example.com"))
                        .with(csrf())
                        .param("host", "imap.example.com")
                        .param("port", "993")
                        .param("ssl", "true")
                        .param("username", "user@example.com")
                        .param("password", "wrong"))
                .andExpect(status().isOk())
                .andExpect(view().name("mailboxes/new"))
                .andExpect(model().attributeHasErrors("mailboxForm"));
    }

    @Test
    void planLimitExceptionRedirectsToThreadsWithUpgradeModalFlash() throws Exception {
        when(mailAccountService.connect(any(User.class), anyString(), anyInt(), anyBoolean(),
                anyString(), anyString()))
                .thenThrow(new PlanLimitExceededException(Plan.FREE, PlanLimitKind.MAILBOX_COUNT, 1, 1));

        mockMvc.perform(post("/mailboxes")
                        .with(user("mailbox@example.com"))
                        .with(csrf())
                        .param("host", "imap.example.com")
                        .param("port", "993")
                        .param("ssl", "true")
                        .param("username", "user@example.com")
                        .param("password", "app-pw"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/threads"))
                .andExpect(flash().attributeExists("upgradeModal"));
    }

    @Test
    void postWithoutCsrfIsForbidden() throws Exception {
        mockMvc.perform(post("/mailboxes")
                        .with(user("mailbox@example.com"))
                        .param("host", "imap.example.com")
                        .param("port", "993")
                        .param("username", "user@example.com")
                        .param("password", "app-pw"))
                .andExpect(status().isForbidden());
    }
}
