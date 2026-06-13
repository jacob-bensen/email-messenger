package com.emailmessenger.auth;

import com.emailmessenger.billing.StripeCheckoutGateway;
import com.emailmessenger.billing.StripePortalGateway;
import com.emailmessenger.domain.AuthEvent;
import com.emailmessenger.domain.AuthEventType;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.AuthEventRepository;
import com.emailmessenger.repository.UserRepository;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class AuthEventServiceTest {

    @Autowired AuthEventService authEvents;
    @Autowired AuthEventRepository repository;
    @Autowired UserService userService;
    @Autowired UserRepository users;

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
    void recordPersistsRowWithNormalizedEmailAndUserLink() {
        User user = userService.register("Logger@Example.com", "password1", null);

        authEvents.record(user, "Logger@Example.com",
                AuthEventType.PASSWORD_CHANGED, "203.0.113.7");

        List<AuthEvent> recent = authEvents.recentFor(user, 10);
        assertThat(recent).hasSize(1);
        AuthEvent saved = recent.get(0);
        assertThat(saved.getEmail()).isEqualTo("logger@example.com");
        assertThat(saved.getEventType()).isEqualTo(AuthEventType.PASSWORD_CHANGED);
        assertThat(saved.getIpAddress()).isEqualTo("203.0.113.7");
        assertThat(saved.getUser().getId()).isEqualTo(user.getId());
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void recordForEmailResolvesUserWhenMatchExistsAndLeavesNullOtherwise() {
        User known = userService.register("known@example.com", "password1", null);

        authEvents.recordForEmail("known@example.com",
                AuthEventType.LOGIN_FAILURE, "10.0.0.1");
        authEvents.recordForEmail("ghost@example.com",
                AuthEventType.LOGIN_FAILURE, "10.0.0.2");

        List<AuthEvent> linked = authEvents.recentFor(known, 10);
        assertThat(linked).hasSize(1);
        assertThat(linked.get(0).getEmail()).isEqualTo("known@example.com");

        long ghostRows = repository.findAll().stream()
                .filter(e -> "ghost@example.com".equals(e.getEmail()))
                .count();
        assertThat(ghostRows).isEqualTo(1);
        repository.findAll().stream()
                .filter(e -> "ghost@example.com".equals(e.getEmail()))
                .forEach(e -> assertThat(e.getUser()).isNull());
    }

    @Test
    void recordSkipsBlankEmail() {
        authEvents.recordForEmail("   ", AuthEventType.LOGIN_FAILURE, "10.0.0.1");
        authEvents.recordForEmail(null, AuthEventType.LOGIN_FAILURE, "10.0.0.1");

        assertThat(repository.count()).isZero();
    }

    @Test
    void recentForReturnsEventsNewestFirstAndCapped() {
        User user = userService.register("ordering@example.com", "password1", null);
        for (int i = 0; i < 12; i++) {
            authEvents.record(user, "ordering@example.com",
                    AuthEventType.LOGIN_SUCCESS, "10.0.0." + i);
        }

        List<AuthEvent> recent = authEvents.recentFor(user, 10);

        assertThat(recent).hasSize(10);
        for (int i = 0; i < recent.size() - 1; i++) {
            assertThat(recent.get(i).getCreatedAt())
                    .isAfterOrEqualTo(recent.get(i + 1).getCreatedAt());
        }
        // The newest of the 12 (i=11 → IP 10.0.0.11) is the first entry.
        assertThat(recent.get(0).getIpAddress()).isEqualTo("10.0.0.11");
    }

    @Test
    void recentForReturnsEmptyForNullOrUnsavedUser() {
        assertThat(authEvents.recentFor(null, 10)).isEmpty();
        assertThat(authEvents.recentFor(new User("nobody@example.com", "x", null), 10))
                .isEmpty();
    }
}
