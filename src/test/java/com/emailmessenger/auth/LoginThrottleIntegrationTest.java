package com.emailmessenger.auth;

import com.emailmessenger.billing.BillingService;
import com.emailmessenger.domain.AuthEventType;
import com.emailmessenger.repository.AuthEventRepository;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.service.ReplyService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LoginThrottleIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserService userService;
    @Autowired AuthEventRepository events;
    @Autowired LoginThrottleService throttle;

    @MockitoBean ReplyService replyService;
    @MockitoBean EmailThreadRepository threadRepository;
    @MockitoBean BillingService billingService;
    @MockitoBean org.springframework.mail.javamail.JavaMailSender mailSender;

    @BeforeEach
    void stubMimeFactory() {
        when(mailSender.createMimeMessage())
                .thenReturn(new MimeMessage((Session) null));
    }

    @Test
    void failedLoginRecordsFailureEvent() throws Exception {
        userService.register("audit@example.com", "password1", null);

        mockMvc.perform(post("/login")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("email", "audit@example.com")
                        .param("password", "wrongpass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));

        long failures = events.findAll().stream()
                .filter(e -> e.getEventType() == AuthEventType.LOGIN_FAILURE
                        && "audit@example.com".equals(e.getEmail()))
                .count();
        assertThat(failures).isEqualTo(1);
    }

    @Test
    void successfulLoginRecordsSuccessEvent() throws Exception {
        userService.register("success@example.com", "password1", null);

        mockMvc.perform(post("/login")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("email", "success@example.com")
                        .param("password", "password1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/threads"));

        long successes = events.findAll().stream()
                .filter(e -> e.getEventType() == AuthEventType.LOGIN_SUCCESS
                        && "success@example.com".equals(e.getEmail()))
                .count();
        assertThat(successes).isEqualTo(1);
    }

    @Test
    void thresholdFailuresLockSubsequentAttemptsEvenWithCorrectPassword() throws Exception {
        userService.register("locked@example.com", "password1", null);

        for (int i = 0; i < throttle.getMaxFailures(); i++) {
            mockMvc.perform(post("/login")
                            .with(SecurityMockMvcRequestPostProcessors.csrf())
                            .param("email", "locked@example.com")
                            .param("password", "nope-" + i))
                    .andExpect(redirectedUrl("/login?error"));
        }

        // Now even the correct password is rejected with the locked redirect.
        mockMvc.perform(post("/login")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("email", "locked@example.com")
                        .param("password", "password1"))
                .andExpect(redirectedUrl("/login?error=locked"));

        long lockedRows = events.findAll().stream()
                .filter(e -> e.getEventType() == AuthEventType.ACCOUNT_LOCKED
                        && "locked@example.com".equals(e.getEmail()))
                .count();
        assertThat(lockedRows).isGreaterThanOrEqualTo(1);
    }

    @Test
    void thresholdFailuresFromSameIpAcrossDifferentEmailsLockThatIp() throws Exception {
        for (int i = 0; i < throttle.getMaxFailures(); i++) {
            userService.register("spray-" + i + "@example.com", "password1", null);
            mockMvc.perform(post("/login")
                            .with(SecurityMockMvcRequestPostProcessors.csrf())
                            .param("email", "spray-" + i + "@example.com")
                            .param("password", "wrong"))
                    .andExpect(redirectedUrl("/login?error"));
        }

        // A previously-unseen email from the same IP is now locked too.
        userService.register("fresh@example.com", "password1", null);
        mockMvc.perform(post("/login")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("email", "fresh@example.com")
                        .param("password", "password1"))
                .andExpect(redirectedUrl("/login?error=locked"));
    }
}
