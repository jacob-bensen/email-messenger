package com.emailmessenger.auth;

import com.emailmessenger.billing.StripeCheckoutGateway;
import com.emailmessenger.billing.StripePortalGateway;
import com.emailmessenger.domain.EmailVerificationToken;
import com.emailmessenger.domain.PasswordResetToken;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.EmailVerificationTokenRepository;
import com.emailmessenger.repository.PasswordResetTokenRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class AccountServiceTest {

    @Autowired AccountService accountService;
    @Autowired UserService userService;
    @Autowired UserRepository users;
    @Autowired PasswordResetTokenRepository passwordResetTokens;
    @Autowired EmailVerificationTokenRepository emailVerificationTokens;
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

    private User register(String email) {
        userService.register(email, "password1", "Test");
        return users.findByEmail(email).orElseThrow();
    }

    // ---------- changePassword ----------

    @Test
    void changePasswordWithCorrectCurrentSwapsHash() {
        User user = register("pw@example.com");

        AccountService.PasswordChangeOutcome outcome =
                accountService.changePassword(user, "password1", "brand-new-12");

        assertThat(outcome).isEqualTo(AccountService.PasswordChangeOutcome.OK);
        User reloaded = users.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("brand-new-12", reloaded.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("password1", reloaded.getPasswordHash())).isFalse();
    }

    @Test
    void changePasswordWithWrongCurrentRejects() {
        User user = register("wrong@example.com");

        AccountService.PasswordChangeOutcome outcome =
                accountService.changePassword(user, "not-the-password", "brand-new-12");

        assertThat(outcome).isEqualTo(AccountService.PasswordChangeOutcome.CURRENT_INCORRECT);
        User reloaded = users.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("password1", reloaded.getPasswordHash())).isTrue();
    }

    @Test
    void changePasswordWithShortNewRejects() {
        User user = register("short@example.com");

        AccountService.PasswordChangeOutcome outcome =
                accountService.changePassword(user, "password1", "short");

        assertThat(outcome).isEqualTo(AccountService.PasswordChangeOutcome.NEW_TOO_SHORT);
        User reloaded = users.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("password1", reloaded.getPasswordHash())).isTrue();
    }

    @Test
    void changePasswordRevokesOutstandingResetAndVerificationTokens() {
        User user = register("revoke@example.com");
        PasswordResetToken pr = passwordResetTokens.save(new PasswordResetToken(
                user, "hash-pr-" + user.getId(),
                LocalDateTime.now().plusHours(1)));
        EmailVerificationToken ev = emailVerificationTokens.save(new EmailVerificationToken(
                user, "hash-ev-" + user.getId(),
                LocalDateTime.now().plusHours(24)));

        accountService.changePassword(user, "password1", "brand-new-12");

        assertThat(passwordResetTokens.findById(pr.getId()).orElseThrow().getUsedAt()).isNotNull();
        assertThat(emailVerificationTokens.findById(ev.getId()).orElseThrow().getUsedAt()).isNotNull();
    }

    // ---------- changeEmail ----------

    @Test
    void changeEmailWithCorrectCurrentSwapsAddressAndClearsVerification() {
        User user = register("old@example.com");
        user.setEmailVerifiedAt(LocalDateTime.now().minusDays(1));
        users.save(user);

        AccountService.EmailChangeOutcome outcome =
                accountService.changeEmail(user, "password1", "new@example.com");

        assertThat(outcome).isEqualTo(AccountService.EmailChangeOutcome.OK);
        User reloaded = users.findById(user.getId()).orElseThrow();
        assertThat(reloaded.getEmail()).isEqualTo("new@example.com");
        assertThat(reloaded.getEmailVerifiedAt()).isNull();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void changeEmailNormalisesWhitespaceAndCase() {
        User user = register("norm@example.com");

        AccountService.EmailChangeOutcome outcome =
                accountService.changeEmail(user, "password1", "  Mixed.Case@Example.COM  ");

        assertThat(outcome).isEqualTo(AccountService.EmailChangeOutcome.OK);
        User reloaded = users.findById(user.getId()).orElseThrow();
        assertThat(reloaded.getEmail()).isEqualTo("mixed.case@example.com");
    }

    @Test
    void changeEmailWithWrongCurrentRejects() {
        User user = register("auth@example.com");

        AccountService.EmailChangeOutcome outcome =
                accountService.changeEmail(user, "wrong-password", "new@example.com");

        assertThat(outcome).isEqualTo(AccountService.EmailChangeOutcome.CURRENT_INCORRECT);
        User reloaded = users.findById(user.getId()).orElseThrow();
        assertThat(reloaded.getEmail()).isEqualTo("auth@example.com");
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void changeEmailToSameAddressReturnsNoChange() {
        User user = register("same@example.com");

        AccountService.EmailChangeOutcome outcome =
                accountService.changeEmail(user, "password1", "same@example.com");

        assertThat(outcome).isEqualTo(AccountService.EmailChangeOutcome.NO_CHANGE);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void changeEmailToTakenAddressRejects() {
        register("incumbent@example.com");
        User user = register("changer@example.com");

        AccountService.EmailChangeOutcome outcome =
                accountService.changeEmail(user, "password1", "incumbent@example.com");

        assertThat(outcome).isEqualTo(AccountService.EmailChangeOutcome.EMAIL_TAKEN);
        User reloaded = users.findById(user.getId()).orElseThrow();
        assertThat(reloaded.getEmail()).isEqualTo("changer@example.com");
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void changeEmailWithInvalidFormatRejects() {
        User user = register("bad@example.com");

        AccountService.EmailChangeOutcome outcome =
                accountService.changeEmail(user, "password1", "not-an-email");

        assertThat(outcome).isEqualTo(AccountService.EmailChangeOutcome.EMAIL_INVALID);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void changeEmailRevokesOutstandingResetAndVerificationTokens() {
        User user = register("revoke-e@example.com");
        PasswordResetToken pr = passwordResetTokens.save(new PasswordResetToken(
                user, "hash-e-pr-" + user.getId(),
                LocalDateTime.now().plusHours(1)));
        EmailVerificationToken ev = emailVerificationTokens.save(new EmailVerificationToken(
                user, "hash-e-ev-" + user.getId(),
                LocalDateTime.now().plusHours(24)));

        accountService.changeEmail(user, "password1", "fresh@example.com");

        assertThat(passwordResetTokens.findById(pr.getId()).orElseThrow().getUsedAt()).isNotNull();
        // The original verification token issued before the change is
        // revoked; the new send mints a fresh one, so the user ends with
        // exactly one outstanding verification row for the new address.
        assertThat(emailVerificationTokens.findById(ev.getId()).orElseThrow().getUsedAt()).isNotNull();
    }

    @Test
    void changeEmailDispatchesVerificationMailToNewAddress() throws Exception {
        User user = register("dispatch@example.com");

        accountService.changeEmail(user, "password1", "shiny@example.com");

        org.mockito.ArgumentCaptor<MimeMessage> captor =
                org.mockito.ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage mime = captor.getValue();
        assertThat(mime.getAllRecipients()[0].toString()).isEqualTo("shiny@example.com");
        assertThat(mime.getSubject()).isEqualTo("Confirm your ConexusMail email address");
    }
}
