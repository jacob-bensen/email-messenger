package com.emailmessenger.auth;

import com.emailmessenger.billing.StripeCheckoutGateway;
import com.emailmessenger.billing.StripePortalGateway;
import com.emailmessenger.domain.EmailVerificationToken;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.EmailVerificationTokenRepository;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
class EmailVerificationControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserService userService;
    @Autowired UserRepository users;
    @Autowired EmailVerificationTokenRepository tokens;

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
    void registrationTriggersVerificationEmail() throws Exception {
        mockMvc.perform(post("/register")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("email", "newuser@example.com")
                        .param("password", "password1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/threads"));

        User saved = users.findByEmail("newuser@example.com").orElseThrow();
        assertThat(saved.getEmailVerifiedAt()).isNull();
        assertThat(tokens.count()).isEqualTo(1);
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void verifyWithValidTokenFlipsUserAndRendersVerified() throws Exception {
        User user = userService.register("verify@example.com", "password1", null);
        String plain = issueRawToken(user);

        mockMvc.perform(get("/verify-email").param("token", plain))
                .andExpect(status().isOk())
                .andExpect(view().name("verify-email"))
                .andExpect(model().attribute("status", "verified"));

        assertThat(users.findById(user.getId()).orElseThrow().getEmailVerifiedAt()).isNotNull();
    }

    @Test
    void verifyWithInvalidTokenRendersInvalid() throws Exception {
        mockMvc.perform(get("/verify-email").param("token", "garbage"))
                .andExpect(status().isOk())
                .andExpect(view().name("verify-email"))
                .andExpect(model().attribute("status", "invalid"));
    }

    @Test
    void verifyEmailEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/verify-email").param("token", "doesnt-matter"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "resend@example.com")
    void resendForSignedInUnverifiedUserRedirectsToThreads() throws Exception {
        userService.register("resend@example.com", "password1", null);

        mockMvc.perform(post("/verify-email/resend")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/threads"));

        assertThat(tokens.count()).isEqualTo(1);
    }

    @Test
    void resendForAnonymousRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/verify-email/resend")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    private String issueRawToken(User user) {
        String plain = "fixture-verify-" + user.getId();
        tokens.save(new EmailVerificationToken(
                user,
                EmailVerificationService.sha256Hex(plain),
                LocalDateTime.now(ZoneOffset.UTC).plusHours(1)));
        return plain;
    }
}
