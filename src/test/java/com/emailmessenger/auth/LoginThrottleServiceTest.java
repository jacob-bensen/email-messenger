package com.emailmessenger.auth;

import com.emailmessenger.billing.StripeCheckoutGateway;
import com.emailmessenger.billing.StripePortalGateway;
import com.emailmessenger.domain.AuthEvent;
import com.emailmessenger.domain.AuthEventType;
import com.emailmessenger.repository.AuthEventRepository;
import com.emailmessenger.service.ReplyService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class LoginThrottleServiceTest {

    @Autowired LoginThrottleService throttle;
    @Autowired AuthEventRepository events;

    @MockBean JavaMailSender mailSender;
    @MockBean StripeCheckoutGateway stripeCheckout;
    @MockBean StripePortalGateway stripePortal;
    @MockBean ReplyService replyService;

    @BeforeEach
    void stubMimeFactory() {
        when(mailSender.createMimeMessage())
                .thenReturn(new MimeMessage((Session) null));
    }

    private void recordFailure(String email, String ip, LocalDateTime when) {
        AuthEvent e = new AuthEvent(null, email, AuthEventType.LOGIN_FAILURE, ip);
        e.setCreatedAt(when);
        events.save(e);
    }

    @Test
    void freshEmailIsNotLocked() {
        assertThat(throttle.isLocked("clean@example.com", "10.0.0.1")).isFalse();
    }

    @Test
    void fiveFailuresInWindowForEmailLocks() {
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < throttle.getMaxFailures(); i++) {
            recordFailure("victim@example.com", "10.0.0." + i, now.minusMinutes(i));
        }

        assertThat(throttle.isLocked("victim@example.com", "203.0.113.99")).isTrue();
    }

    @Test
    void fourFailuresInWindowDoesNotLock() {
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < throttle.getMaxFailures() - 1; i++) {
            recordFailure("survivor@example.com", "10.0.0." + i, now.minusMinutes(i));
        }

        assertThat(throttle.isLocked("survivor@example.com", "203.0.113.50")).isFalse();
    }

    @Test
    void oldFailuresOutsideWindowDoNotLock() {
        LocalDateTime ancient = LocalDateTime.now()
                .minus(throttle.getWindow().plusMinutes(5));
        for (int i = 0; i < throttle.getMaxFailures() + 3; i++) {
            recordFailure("old@example.com", "10.0.0." + i, ancient.minusMinutes(i));
        }

        assertThat(throttle.isLocked("old@example.com", "203.0.113.50")).isFalse();
    }

    @Test
    void ipThresholdLocksRegardlessOfEmail() {
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < throttle.getMaxFailures(); i++) {
            recordFailure("rotating-" + i + "@example.com", "198.51.100.7", now.minusSeconds(i));
        }

        assertThat(throttle.isLocked("unseen@example.com", "198.51.100.7")).isTrue();
    }

    @Test
    void emailNormalizationMatchesAcrossCase() {
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < throttle.getMaxFailures(); i++) {
            recordFailure("mixed@example.com", "10.0.0." + i, now.minusSeconds(i));
        }

        assertThat(throttle.isLocked("Mixed@Example.COM", "203.0.113.1")).isTrue();
    }

    @Test
    void blankInputsReturnNotLocked() {
        assertThat(throttle.isLocked(null, null)).isFalse();
        assertThat(throttle.isLocked("", "  ")).isFalse();
    }
}
