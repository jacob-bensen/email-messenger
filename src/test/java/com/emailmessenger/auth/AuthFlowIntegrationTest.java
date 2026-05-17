package com.emailmessenger.auth;

import com.emailmessenger.billing.BillingService;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.service.ReplyService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class AuthFlowIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository users;
    @Autowired UserService userService;

    // The mail-sending side path is irrelevant to auth and avoids a real SMTP host.
    @MockBean ReplyService replyService;
    @MockBean EmailThreadRepository threadRepository;
    // Stripe is unconfigured in the dev profile; mock so happy-path checkout
    // is deterministic without hitting the real gateway.
    @MockBean BillingService billingService;

    @Test
    void loginPageIsPublic() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void pricingPageIsPublic() throws Exception {
        mockMvc.perform(get("/pricing"))
                .andExpect(status().isOk())
                .andExpect(view().name("pricing"));
    }

    @Test
    void threadsRedirectsAnonymousToLogin() throws Exception {
        mockMvc.perform(get("/threads"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void registrationCreatesUserAndAutoLogsIn() throws Exception {
        mockMvc.perform(post("/register")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("email", "new@example.com")
                        .param("password", "password1")
                        .param("displayName", "New User"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/threads"));

        assertThat(users.findByEmail("new@example.com")).isPresent();
    }

    @Test
    void registrationWithInvalidEmailReturnsForm() throws Exception {
        mockMvc.perform(post("/register")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("email", "not-an-email")
                        .param("password", "password1"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));

        assertThat(users.findByEmail("not-an-email")).isEmpty();
    }

    @Test
    void registrationWithShortPasswordReturnsForm() throws Exception {
        mockMvc.perform(post("/register")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("email", "short@example.com")
                        .param("password", "abc"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));

        assertThat(users.findByEmail("short@example.com")).isEmpty();
    }

    @Test
    void registrationWithDuplicateEmailReturnsForm() throws Exception {
        userService.register("dup@example.com", "password1", null);

        mockMvc.perform(post("/register")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("email", "dup@example.com")
                        .param("password", "password2"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void loginWithCorrectCredentialsRedirectsToThreads() throws Exception {
        userService.register("login@example.com", "password1", null);

        mockMvc.perform(post("/login")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("email", "login@example.com")
                        .param("password", "password1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/threads"));
    }

    @Test
    void loginWithWrongPasswordRedirectsToErrorPage() throws Exception {
        userService.register("login2@example.com", "password1", null);

        mockMvc.perform(post("/login")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("email", "login2@example.com")
                        .param("password", "wrongpass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void registrationWithPlanStartsCheckoutAndRedirectsToStripe() throws Exception {
        when(billingService.startCheckout(any(User.class), eq(Plan.PERSONAL)))
                .thenReturn("https://checkout.stripe.com/c/pay/cs_test_funnel");

        mockMvc.perform(post("/register")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("email", "trial@example.com")
                        .param("password", "password1")
                        .param("plan", "personal"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://checkout.stripe.com/c/pay/cs_test_funnel"));

        assertThat(users.findByEmail("trial@example.com")).isPresent();
        verify(billingService).startCheckout(any(User.class), eq(Plan.PERSONAL));
    }

    @Test
    void registrationWithUnknownPlanFallsThroughToThreads() throws Exception {
        mockMvc.perform(post("/register")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("email", "tampered@example.com")
                        .param("password", "password1")
                        .param("plan", "platinum"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/threads"));

        assertThat(users.findByEmail("tampered@example.com")).isPresent();
        verify(billingService, never()).startCheckout(any(User.class), any(Plan.class));
    }
}
