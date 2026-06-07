package com.emailmessenger.auth;

import com.emailmessenger.domain.User;
import com.emailmessenger.repository.EmailVerificationTokenRepository;
import com.emailmessenger.repository.PasswordResetTokenRepository;
import com.emailmessenger.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * In-app self-serve for the signed-in user's password and email.
 *
 * <p>Both flows require the user's current password. On a successful
 * change every outstanding password-reset and email-verification token
 * for the user is revoked, so a previously-leaked reset URL or a stale
 * verification link cannot be replayed against the new credentials.
 *
 * <p>{@link #changeEmail} additionally clears {@code email_verified_at}
 * and dispatches a fresh verification email to the new address. The
 * controller is responsible for ending the current session — the
 * SecurityContext still points at the old username and would otherwise
 * blow up on the next request.
 */
@Service
public class AccountService {

    public enum PasswordChangeOutcome { OK, CURRENT_INCORRECT, NEW_TOO_SHORT }

    public enum EmailChangeOutcome {
        OK, CURRENT_INCORRECT, EMAIL_INVALID, EMAIL_TAKEN, NO_CHANGE
    }

    static final int MIN_PASSWORD_LENGTH = 8;
    static final int MAX_EMAIL_LENGTH = 254;

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository passwordResetTokens;
    private final EmailVerificationTokenRepository emailVerificationTokens;
    private final EmailVerificationService emailVerificationService;
    private final Clock clock;

    AccountService(UserRepository users,
                   PasswordEncoder passwordEncoder,
                   PasswordResetTokenRepository passwordResetTokens,
                   EmailVerificationTokenRepository emailVerificationTokens,
                   EmailVerificationService emailVerificationService,
                   Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetTokens = passwordResetTokens;
        this.emailVerificationTokens = emailVerificationTokens;
        this.emailVerificationService = emailVerificationService;
        this.clock = clock;
    }

    @Transactional
    public PasswordChangeOutcome changePassword(User user,
                                                String currentPassword,
                                                String newPassword) {
        if (newPassword == null || newPassword.length() < MIN_PASSWORD_LENGTH) {
            return PasswordChangeOutcome.NEW_TOO_SHORT;
        }
        if (currentPassword == null
                || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            return PasswordChangeOutcome.CURRENT_INCORRECT;
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        users.save(user);
        LocalDateTime now = LocalDateTime.now(clock);
        passwordResetTokens.markAllUsedFor(user, now);
        emailVerificationTokens.markAllUsedFor(user, now);
        return PasswordChangeOutcome.OK;
    }

    @Transactional
    public EmailChangeOutcome changeEmail(User user,
                                          String currentPassword,
                                          String newEmail) {
        if (currentPassword == null
                || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            return EmailChangeOutcome.CURRENT_INCORRECT;
        }
        String normalized = UserService.normalizeEmail(newEmail);
        if (normalized == null || normalized.isBlank()
                || normalized.length() > MAX_EMAIL_LENGTH
                || !looksLikeEmail(normalized)) {
            return EmailChangeOutcome.EMAIL_INVALID;
        }
        if (normalized.equals(user.getEmail())) {
            return EmailChangeOutcome.NO_CHANGE;
        }
        if (users.existsByEmail(normalized)) {
            return EmailChangeOutcome.EMAIL_TAKEN;
        }
        user.setEmail(normalized);
        user.setEmailVerifiedAt(null);
        users.save(user);
        LocalDateTime now = LocalDateTime.now(clock);
        passwordResetTokens.markAllUsedFor(user, now);
        emailVerificationTokens.markAllUsedFor(user, now);
        emailVerificationService.sendVerification(user);
        return EmailChangeOutcome.OK;
    }

    private static boolean looksLikeEmail(String s) {
        int at = s.indexOf('@');
        return at > 0 && at < s.length() - 1 && s.indexOf('@', at + 1) < 0;
    }
}
