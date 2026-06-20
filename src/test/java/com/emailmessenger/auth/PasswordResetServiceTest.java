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
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
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
@Transactional
class PasswordResetServiceTest {

    @Autowired PasswordResetService passwordResetService;
    @Autowired UserService userService;
    @Autowired UserRepository users;
    @Autowired PasswordResetTokenRepository tokens;
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

    private User newUser(String email) {
        userService.register(email, "old-password1", "Owner");
        return users.findByEmail(email).orElseThrow();
    }

    @Test
    void requestResetForKnownUserSendsEmailWithTokenUrlAndPersistsHash() throws Exception {
        User user = newUser("known@example.com");

        PasswordResetService.Outcome outcome = passwordResetService.requestReset("Known@Example.com");

        assertThat(outcome).isEqualTo(PasswordResetService.Outcome.SENT);
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage mime = captor.getValue();
        assertThat(mime.getAllRecipients()).hasSize(1);
        assertThat(mime.getAllRecipients()[0].toString()).isEqualTo("known@example.com");
        assertThat(mime.getSubject()).isEqualTo("Reset your ConexusMail password");
        String body = (String) mime.getContent();
        assertThat(body).contains("/password/reset?token=");

        // Plaintext token is in the body; DB stores its hash.
        String plain = extractTokenFromBody(body);
        assertThat(plain).isNotBlank();
        List<PasswordResetToken> rows = tokens.findAll();
        assertThat(rows).hasSize(1);
        PasswordResetToken row = rows.get(0);
        assertThat(row.getTokenHash()).isEqualTo(PasswordResetService.sha256Hex(plain));
        assertThat(row.getTokenHash()).isNotEqualTo(plain);
        assertThat(row.getUser().getId()).isEqualTo(user.getId());
        assertThat(row.getUsedAt()).isNull();
        assertThat(row.getExpiresAt()).isAfter(LocalDateTime.now().plusMinutes(55));
    }

    @Test
    void requestResetForUnknownEmailIsSilentNoOp() {
        PasswordResetService.Outcome outcome = passwordResetService.requestReset("ghost@example.com");

        assertThat(outcome).isEqualTo(PasswordResetService.Outcome.IGNORED);
        verify(mailSender, never()).send(any(MimeMessage.class));
        assertThat(tokens.count()).isZero();
    }

    @Test
    void requestResetForDisabledUserIsSilentNoOp() {
        User user = newUser("disabled@example.com");
        user.setEnabled(false);
        users.save(user);

        PasswordResetService.Outcome outcome = passwordResetService.requestReset("disabled@example.com");

        assertThat(outcome).isEqualTo(PasswordResetService.Outcome.IGNORED);
        verify(mailSender, never()).send(any(MimeMessage.class));
        assertThat(tokens.count()).isZero();
    }

    @Test
    void requestResetForGoogleOnlyUserReturnsGoogleOnlyOutcomeAndSendsNoEmail() {
        User user = newUser("g@example.com");
        user.setGoogleSubject("sub-789");
        user.setPasswordSet(false);
        users.save(user);

        PasswordResetService.Outcome outcome = passwordResetService.requestReset("g@example.com");

        assertThat(outcome).isEqualTo(PasswordResetService.Outcome.GOOGLE_ONLY);
        verify(mailSender, never()).send(any(MimeMessage.class));
        assertThat(tokens.count()).isZero();
    }

    @Test
    void requestResetForGoogleLinkedUserWhoChoseAPasswordStillSendsResetEmail() {
        // An email-password user later clicks "Continue with Google", linking
        // the row. They still know a password they chose, so /password/forgot
        // must work the same as for any other email-password user.
        User user = newUser("linked@example.com");
        user.setGoogleSubject("sub-linked");
        users.save(user);

        PasswordResetService.Outcome outcome = passwordResetService.requestReset("linked@example.com");

        assertThat(outcome).isEqualTo(PasswordResetService.Outcome.SENT);
        verify(mailSender).send(any(MimeMessage.class));
        assertThat(tokens.count()).isOne();
    }

    @Test
    void consumingResetTokenStampsPasswordSetTrueOnUser() throws Exception {
        // Email-password row that for some reason has password_set=false
        // (e.g. an admin tool forced it back). Completing a reset must
        // mark them as having a known password again.
        User user = newUser("goog-then-pw@example.com");
        passwordResetService.requestReset("goog-then-pw@example.com");
        String plain = extractTokenFromCapturedEmail();
        user.setPasswordSet(false);
        users.save(user);

        assertThat(passwordResetService.consumeToken(plain, "i-chose-this")).isPresent();
        User reloaded = users.findById(user.getId()).orElseThrow();
        assertThat(reloaded.isPasswordSet()).isTrue();
        assertThat(reloaded.isGoogleOnly()).isFalse();
    }

    @Test
    void consumeTokenSetsNewPasswordAndMarksTokenUsed() throws Exception {
        User user = newUser("consume@example.com");
        passwordResetService.requestReset("consume@example.com");
        String plain = extractTokenFromCapturedEmail();

        Optional<User> result = passwordResetService.consumeToken(plain, "brand-new-pw");

        assertThat(result).isPresent();
        User reloaded = users.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("brand-new-pw", reloaded.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("old-password1", reloaded.getPasswordHash())).isFalse();

        PasswordResetToken row = tokens.findAll().get(0);
        assertThat(row.getUsedAt()).isNotNull();
    }

    @Test
    void consumeRevokesAllOtherOutstandingTokensForSameUser() throws Exception {
        User user = newUser("revoke@example.com");
        passwordResetService.requestReset("revoke@example.com");
        String firstPlain = extractTokenFromCapturedEmail();
        // Simulate "I lost the first email, send another."
        resetMailMock();
        passwordResetService.requestReset("revoke@example.com");
        String secondPlain = extractTokenFromCapturedEmail();

        assertThat(tokens.count()).isEqualTo(2);

        // Use the second link.
        assertThat(passwordResetService.consumeToken(secondPlain, "fresh-pw1")).isPresent();

        // The first one must now be dead, even though it hasn't expired.
        assertThat(passwordResetService.findUserForValidToken(firstPlain)).isEmpty();
        assertThat(passwordResetService.consumeToken(firstPlain, "another-pw1")).isEmpty();
        // Password must remain the one set via the second link.
        User reloaded = users.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("fresh-pw1", reloaded.getPasswordHash())).isTrue();
    }

    @Test
    void consumeExpiredTokenFails() {
        User user = newUser("expired@example.com");
        // Insert a directly-expired token so we don't have to wait an hour.
        String plain = "manual-test-token-for-expiry";
        PasswordResetToken expired = tokens.save(new PasswordResetToken(
                user, PasswordResetService.sha256Hex(plain),
                LocalDateTime.now().minusMinutes(1)));

        assertThat(passwordResetService.findUserForValidToken(plain)).isEmpty();
        assertThat(passwordResetService.consumeToken(plain, "doesnt-matter")).isEmpty();
        // And the row is untouched (still un-used) — service didn't mark it.
        assertThat(tokens.findById(expired.getId()).orElseThrow().getUsedAt()).isNull();
    }

    @Test
    void consumeRejectsShortPasswordWithoutBurningTheToken() throws Exception {
        newUser("short@example.com");
        passwordResetService.requestReset("short@example.com");
        String plain = extractTokenFromCapturedEmail();

        assertThat(passwordResetService.consumeToken(plain, "short")).isEmpty();

        // Token is still valid for a real attempt.
        assertThat(passwordResetService.findUserForValidToken(plain)).isPresent();
    }

    @Test
    void consumeUnknownTokenFails() {
        assertThat(passwordResetService.consumeToken("never-issued", "password-1234"))
                .isEmpty();
    }

    @Test
    void findUserForValidTokenReturnsOwnerForFreshToken() throws Exception {
        User user = newUser("fresh@example.com");
        passwordResetService.requestReset("fresh@example.com");
        String plain = extractTokenFromCapturedEmail();

        assertThat(passwordResetService.findUserForValidToken(plain))
                .map(User::getId)
                .contains(user.getId());
    }

    private String extractTokenFromCapturedEmail() throws Exception {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        String body = (String) captor.getValue().getContent();
        return extractTokenFromBody(body);
    }

    private static String extractTokenFromBody(String body) {
        int idx = body.indexOf("/password/reset?token=");
        if (idx < 0) return null;
        int start = idx + "/password/reset?token=".length();
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
