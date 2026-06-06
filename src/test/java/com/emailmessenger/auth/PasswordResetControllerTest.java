package com.emailmessenger.auth;

import com.emailmessenger.billing.StripeCheckoutGateway;
import com.emailmessenger.billing.StripePortalGateway;
import com.emailmessenger.domain.PasswordResetToken;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.PasswordResetTokenRepository;
import com.emailmessenger.repository.UserRepository;
import com.emailmessenger.service.ReplyService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class PasswordResetControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserService userService;
    @Autowired UserRepository users;
    @Autowired PasswordResetTokenRepository tokens;
    @Autowired PasswordEncoder passwordEncoder;

    @MockBean JavaMailSender mailSender;
    @MockBean StripeCheckoutGateway stripeCheckout;
    @MockBean StripePortalGateway stripePortal;
    @MockBean ReplyService replyService;

    @BeforeEach
    void stubMimeFactory() {
        when(mailSender.createMimeMessage())
                .thenReturn(new MimeMessage((Session) null));
    }

    @Test
    void forgotFormIsPublicAndRendersForm() throws Exception {
        mockMvc.perform(get("/password/forgot"))
                .andExpect(status().isOk())
                .andExpect(view().name("password/forgot"));
    }

    @Test
    void postForgotForUnknownEmailRendersGenericSentScreenWithNoMailSent() throws Exception {
        mockMvc.perform(post("/password/forgot")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("email", "ghost@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("password/forgot"))
                .andExpect(model().attribute("status", "sent"));

        verify(mailSender, never()).send(any(MimeMessage.class));
        assertThat(tokens.count()).isZero();
    }

    @Test
    void postForgotForKnownEmailRendersGenericSentScreenAndSendsMail() throws Exception {
        userService.register("real@example.com", "password1", "Real");

        mockMvc.perform(post("/password/forgot")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("email", "real@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("password/forgot"))
                .andExpect(model().attribute("status", "sent"));

        verify(mailSender).send(any(MimeMessage.class));
        assertThat(tokens.count()).isOne();
    }

    @Test
    void resetFormWithValidTokenRendersForm() throws Exception {
        User user = userService.register("rf@example.com", "password1", "RF");
        String plain = issueRawToken(user);

        mockMvc.perform(get("/password/reset").param("token", plain))
                .andExpect(status().isOk())
                .andExpect(view().name("password/reset"))
                .andExpect(model().attribute("token", plain))
                .andExpect(model().attributeDoesNotExist("status"));
    }

    @Test
    void resetFormWithUnknownTokenRendersInvalidStatus() throws Exception {
        mockMvc.perform(get("/password/reset").param("token", "not-real"))
                .andExpect(status().isOk())
                .andExpect(view().name("password/reset"))
                .andExpect(model().attribute("status", "invalid"));
    }

    @Test
    void postResetWithValidTokenAndNewPasswordRedirectsToLogin() throws Exception {
        User user = userService.register("post@example.com", "password1", "Post");
        String plain = issueRawToken(user);

        mockMvc.perform(post("/password/reset")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("token", plain)
                        .param("password", "brand-new-12"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?reset"));

        User reloaded = users.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("brand-new-12", reloaded.getPasswordHash())).isTrue();
    }

    @Test
    void postResetWithShortPasswordRendersFormError() throws Exception {
        User user = userService.register("ps@example.com", "password1", "PS");
        String plain = issueRawToken(user);

        mockMvc.perform(post("/password/reset")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("token", plain)
                        .param("password", "short"))
                .andExpect(status().isOk())
                .andExpect(view().name("password/reset"))
                .andExpect(model().attribute("token", plain))
                .andExpect(model().attribute("error", "tooShort"));

        // Password unchanged on disk.
        User reloaded = users.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("password1", reloaded.getPasswordHash())).isTrue();
    }

    @Test
    void postResetWithInvalidTokenRendersInvalidStatus() throws Exception {
        mockMvc.perform(post("/password/reset")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("token", "not-real-anymore")
                        .param("password", "long-enough-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("password/reset"))
                .andExpect(model().attribute("status", "invalid"));
    }

    private String issueRawToken(User user) {
        String plain = "fixture-token-" + user.getId();
        tokens.save(new PasswordResetToken(
                user,
                PasswordResetService.sha256Hex(plain),
                LocalDateTime.now().plusMinutes(30)));
        return plain;
    }
}
