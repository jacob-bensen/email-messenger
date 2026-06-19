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
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class EmailVerificationServiceTest {

    @Autowired EmailVerificationService emailVerificationService;
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

    private User newUnverifiedUser(String email) {
        userService.register(email, "password1", "New");
        User user = users.findByEmail(email).orElseThrow();
        // Tests fix this explicitly because the dev H2 backfill in V15 only
        // runs once at startup; new registrations leave email_verified_at
        // null, which is exactly what we want.
        assertThat(user.getEmailVerifiedAt()).isNull();
        return user;
    }

    @Test
    void sendVerificationMintsTokenAndEmailsUrl() throws Exception {
        User user = newUnverifiedUser("verify@example.com");

        boolean sent = emailVerificationService.sendVerification(user);

        assertThat(sent).isTrue();
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage mime = captor.getValue();
        assertThat(mime.getAllRecipients()[0].toString()).isEqualTo("verify@example.com");
        assertThat(mime.getSubject()).isEqualTo("Confirm your ConexusMail email address");
        String body = (String) mime.getContent();
        assertThat(body).contains("/verify-email?token=");

        String plain = extractTokenFromBody(body);
        assertThat(plain).isNotBlank();
        List<EmailVerificationToken> rows = tokens.findAll();
        assertThat(rows).hasSize(1);
        EmailVerificationToken row = rows.get(0);
        assertThat(row.getTokenHash()).isEqualTo(EmailVerificationService.sha256Hex(plain));
        assertThat(row.getTokenHash()).isNotEqualTo(plain);
        assertThat(row.getUser().getId()).isEqualTo(user.getId());
        assertThat(row.getUsedAt()).isNull();
        // 24h TTL — assert at least 23h to leave slack for slow CI.
        assertThat(row.getExpiresAt()).isAfter(LocalDateTime.now().plusHours(23));
    }

    @Test
    void sendVerificationForAlreadyVerifiedUserIsNoOp() {
        User user = newUnverifiedUser("already@example.com");
        user.setEmailVerifiedAt(LocalDateTime.now().minusDays(2));
        users.save(user);

        boolean sent = emailVerificationService.sendVerification(user);

        assertThat(sent).isFalse();
        verify(mailSender, never()).send(any(MimeMessage.class));
        assertThat(tokens.count()).isZero();
    }

    @Test
    void verifyConsumesTokenAndSetsEmailVerifiedAt() throws Exception {
        User user = newUnverifiedUser("consume@example.com");
        emailVerificationService.sendVerification(user);
        String plain = extractTokenFromCapturedEmail();

        Optional<User> result = emailVerificationService.verify(plain);

        assertThat(result).isPresent();
        User reloaded = users.findById(user.getId()).orElseThrow();
        assertThat(reloaded.getEmailVerifiedAt()).isNotNull();
        EmailVerificationToken row = tokens.findAll().get(0);
        assertThat(row.getUsedAt()).isNotNull();
    }

    @Test
    void verifyRevokesAllOutstandingTokensForUser() throws Exception {
        User user = newUnverifiedUser("revoke@example.com");
        emailVerificationService.sendVerification(user);
        String firstPlain = extractTokenFromCapturedEmail();
        resetMailMock();
        emailVerificationService.sendVerification(user);
        String secondPlain = extractTokenFromCapturedEmail();

        assertThat(tokens.count()).isEqualTo(2);

        assertThat(emailVerificationService.verify(firstPlain)).isPresent();
        // Second token can no longer verify — the first round revoked it.
        assertThat(emailVerificationService.verify(secondPlain)).isEmpty();
    }

    @Test
    void verifyExpiredTokenFailsAndDoesNotFlipUser() {
        User user = newUnverifiedUser("expired@example.com");
        String plain = "verify-expired-fixture";
        EmailVerificationToken expired = tokens.save(new EmailVerificationToken(
                user, EmailVerificationService.sha256Hex(plain),
                LocalDateTime.now().minusMinutes(1)));

        assertThat(emailVerificationService.verify(plain)).isEmpty();
        assertThat(users.findById(user.getId()).orElseThrow().getEmailVerifiedAt()).isNull();
        assertThat(tokens.findById(expired.getId()).orElseThrow().getUsedAt()).isNull();
    }

    @Test
    void verifyUnknownTokenFails() {
        assertThat(emailVerificationService.verify("never-issued")).isEmpty();
    }

    private String extractTokenFromCapturedEmail() throws Exception {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        String body = (String) captor.getValue().getContent();
        return extractTokenFromBody(body);
    }

    private static String extractTokenFromBody(String body) {
        int idx = body.indexOf("/verify-email?token=");
        if (idx < 0) return null;
        int start = idx + "/verify-email?token=".length();
        int end = start;
        while (end < body.length()) {
            char c = body.charAt(end);
            if (Character.isLetterOrDigit(c) || c == '-' || c == '_') {
                end++;
            } else {
                break;
            }
        }
        return body.substring(start, end);
    }

    private void resetMailMock() {
        org.mockito.Mockito.reset(mailSender);
        when(mailSender.createMimeMessage())
                .thenReturn(new MimeMessage((Session) null));
    }
}
