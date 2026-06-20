package com.emailmessenger.auth;

import com.emailmessenger.billing.BillingPeriod;
import com.emailmessenger.billing.StripeCheckoutGateway;
import com.emailmessenger.billing.StripePortalGateway;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.Subscription;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.SubscriptionRepository;
import com.emailmessenger.repository.UserRepository;
import com.emailmessenger.service.ReplyService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccountControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserService userService;
    @Autowired UserRepository users;
    @Autowired SubscriptionRepository subscriptions;
    @Autowired PasswordEncoder passwordEncoder;

    @MockitoBean JavaMailSender mailSender;
    @MockitoBean StripeCheckoutGateway stripeCheckout;
    @MockitoBean StripePortalGateway stripePortal;
    @MockitoBean ReplyService replyService;

    @BeforeEach
    void stubMimeFactory() {
        when(mailSender.createMimeMessage())
                .thenReturn(new MimeMessage((Session) null));
    }

    @Test
    void accountPageRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/account"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @WithMockUser(username = "ada@example.com")
    void accountPageRendersCurrentUserInfo() throws Exception {
        userService.register("ada@example.com", "password1", "Ada Lovelace");

        mockMvc.perform(get("/account"))
                .andExpect(status().isOk())
                .andExpect(view().name("account"))
                .andExpect(model().attribute("currentEmail", "ada@example.com"))
                .andExpect(model().attribute("displayName", "Ada Lovelace"))
                .andExpect(content().string(containsString("ada@example.com")));
    }

    @Test
    @WithMockUser(username = "pwok@example.com")
    void changePasswordHappyPathFlashesOkAndRedirects() throws Exception {
        User user = userService.register("pwok@example.com", "password1", "PwOk");

        mockMvc.perform(post("/account/password")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("currentPassword", "password1")
                        .param("newPassword", "brand-new-12"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account"))
                .andExpect(flash().attribute("passwordOutcome", "OK"));

        User reloaded = users.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("brand-new-12", reloaded.getPasswordHash())).isTrue();
    }

    @Test
    @WithMockUser(username = "pwwrong@example.com")
    void changePasswordWithWrongCurrentFlashesError() throws Exception {
        User user = userService.register("pwwrong@example.com", "password1", null);

        mockMvc.perform(post("/account/password")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("currentPassword", "nope")
                        .param("newPassword", "brand-new-12"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account"))
                .andExpect(flash().attribute("passwordOutcome", "CURRENT_INCORRECT"));

        User reloaded = users.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("password1", reloaded.getPasswordHash())).isTrue();
    }

    @Test
    @WithMockUser(username = "pwshort@example.com")
    void changePasswordWithShortNewFlashesError() throws Exception {
        userService.register("pwshort@example.com", "password1", null);

        mockMvc.perform(post("/account/password")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("currentPassword", "password1")
                        .param("newPassword", "short"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("passwordOutcome", "NEW_TOO_SHORT"));
    }

    @Test
    void changePasswordRequiresCsrf() throws Exception {
        mockMvc.perform(post("/account/password")
                        .with(SecurityMockMvcRequestPostProcessors.user("noone@example.com"))
                        .param("currentPassword", "password1")
                        .param("newPassword", "brand-new-12"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "emok@example.com")
    void changeEmailHappyPathLogsOutAndRedirectsToLogin() throws Exception {
        User user = userService.register("emok@example.com", "password1", null);

        mockMvc.perform(post("/account/email")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("currentPassword", "password1")
                        .param("newEmail", "fresh@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?email-changed"));

        User reloaded = users.findById(user.getId()).orElseThrow();
        assertThat(reloaded.getEmail()).isEqualTo("fresh@example.com");
        assertThat(reloaded.getEmailVerifiedAt()).isNull();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @WithMockUser(username = "emwrong@example.com")
    void changeEmailWithWrongPasswordFlashesError() throws Exception {
        User user = userService.register("emwrong@example.com", "password1", null);

        mockMvc.perform(post("/account/email")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("currentPassword", "nope")
                        .param("newEmail", "fresh@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account"))
                .andExpect(flash().attribute("emailOutcome", "CURRENT_INCORRECT"));

        User reloaded = users.findById(user.getId()).orElseThrow();
        assertThat(reloaded.getEmail()).isEqualTo("emwrong@example.com");
    }

    @Test
    @WithMockUser(username = "emtaken@example.com")
    void changeEmailToTakenAddressFlashesError() throws Exception {
        userService.register("incumbent@example.com", "password1", null);
        User user = userService.register("emtaken@example.com", "password1", null);

        mockMvc.perform(post("/account/email")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("currentPassword", "password1")
                        .param("newEmail", "incumbent@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account"))
                .andExpect(flash().attribute("emailOutcome", "EMAIL_TAKEN"));

        User reloaded = users.findById(user.getId()).orElseThrow();
        assertThat(reloaded.getEmail()).isEqualTo("emtaken@example.com");
    }

    @Test
    @WithMockUser(username = "billed@example.com")
    void accountPageShowsActiveAnnualCadenceAndRenewalDate() throws Exception {
        User user = userService.register("billed@example.com", "password1", null);
        Subscription sub = new Subscription(user, "cus_billed", "active");
        sub.setPlan(Plan.PERSONAL);
        sub.setBillingPeriod(BillingPeriod.ANNUAL);
        sub.setCurrentPeriodEnd(java.time.LocalDateTime.of(2027, 6, 7, 12, 0));
        subscriptions.save(sub);

        mockMvc.perform(get("/account"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Personal · Annual")))
                .andExpect(content().string(containsString("2027-06-07")));
    }

    @Test
    @WithMockUser(username = "trialing@example.com")
    void accountPageShowsTrialCadenceWithTrialEnd() throws Exception {
        User user = userService.register("trialing@example.com", "password1", null);
        Subscription sub = new Subscription(user, "cus_trial", "trialing");
        sub.setPlan(Plan.PERSONAL);
        sub.setBillingPeriod(BillingPeriod.MONTHLY);
        sub.setTrialEndsAt(java.time.LocalDateTime.of(2026, 6, 22, 12, 0));
        subscriptions.save(sub);

        mockMvc.perform(get("/account"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Personal · Monthly trial")))
                .andExpect(content().string(containsString("2026-06-22")));
    }

    @Test
    @WithMockUser(username = "nosub@example.com")
    void accountPageOmitsSubscriptionCardForFreeUser() throws Exception {
        userService.register("nosub@example.com", "password1", null);

        mockMvc.perform(get("/account"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("billing", (Object) null));
    }

    @Test
    @WithMockUser(username = "eminvalid@example.com")
    void changeEmailWithInvalidFormatFlashesError() throws Exception {
        userService.register("eminvalid@example.com", "password1", null);

        mockMvc.perform(post("/account/email")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("currentPassword", "password1")
                        .param("newEmail", "not-an-email"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("emailOutcome", "EMAIL_INVALID"));
    }
}
