package com.emailmessenger.auth;

import com.emailmessenger.domain.AuthEventType;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.EmailVerificationTokenRepository;
import com.emailmessenger.repository.PasswordResetTokenRepository;
import com.emailmessenger.repository.SubscriptionRepository;
import com.emailmessenger.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
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

    public enum PasswordChangeOutcome { OK, PASSWORD_NOT_SET, CURRENT_INCORRECT, NEW_TOO_SHORT, NEW_TOO_LONG }

    public enum DisplayNameOutcome { OK, TOO_LONG }

    /** Result of {@link #changePassword}; {@code revokedDevices} is how many apps were signed out. */
    public record PasswordChange(PasswordChangeOutcome outcome, int revokedDevices) {}

    public enum EmailChangeOutcome {
        OK, PASSWORD_NOT_SET, CURRENT_INCORRECT, EMAIL_INVALID, EMAIL_TAKEN, NO_CHANGE
    }

    static final int MIN_PASSWORD_LENGTH = 8;
    // BCrypt only reads the first 72 bytes, and Spring's encoder rejects longer input.
    static final int MAX_PASSWORD_BYTES = 72;
    public static final int MAX_DISPLAY_NAME_LENGTH = 100;
    static final int MAX_EMAIL_LENGTH = 254;

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository passwordResetTokens;
    private final EmailVerificationTokenRepository emailVerificationTokens;
    private final EmailVerificationService emailVerificationService;
    private final AuthEventService authEvents;
    private final SubscriptionRepository subscriptions;
    private final SignedInDeviceService devices;
    private final Clock clock;

    AccountService(UserRepository users,
                   PasswordEncoder passwordEncoder,
                   PasswordResetTokenRepository passwordResetTokens,
                   EmailVerificationTokenRepository emailVerificationTokens,
                   EmailVerificationService emailVerificationService,
                   AuthEventService authEvents,
                   SubscriptionRepository subscriptions,
                   SignedInDeviceService devices,
                   Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetTokens = passwordResetTokens;
        this.emailVerificationTokens = emailVerificationTokens;
        this.emailVerificationService = emailVerificationService;
        this.authEvents = authEvents;
        this.subscriptions = subscriptions;
        this.devices = devices;
        this.clock = clock;
    }

    /** The paid-plan line of /account and {@code GET /api/v1/account}; null on Free. */
    @Transactional(readOnly = true)
    public AccountBillingSummary billingSummary(User user) {
        return AccountBillingSummary.from(subscriptions.findByUser(user).orElse(null));
    }

    /** Trimmed; blank clears the name. Shared by /account and {@code PUT /api/v1/account/profile}. */
    @Transactional
    public DisplayNameOutcome changeDisplayName(User user, String displayName) {
        String trimmed = displayName == null ? "" : displayName.strip();
        if (trimmed.length() > MAX_DISPLAY_NAME_LENGTH) {
            return DisplayNameOutcome.TOO_LONG;
        }
        user.setDisplayName(trimmed.isEmpty() ? null : trimmed);
        users.save(user);
        return DisplayNameOutcome.OK;
    }

    /**
     * Also signs out every signed-in app except {@code keepTokenId} (the API
     * caller's own token; null on the website), so a leaked token does not
     * outlive the password it was issued for.
     */
    @Transactional
    public PasswordChange changePassword(User user,
                                         String currentPassword,
                                         String newPassword,
                                         Long keepTokenId) {
        // A Google-provisioned account holds a random hash nobody knows, so
        // "current password" can never be right; say why instead.
        if (!user.isPasswordSet()) {
            return new PasswordChange(PasswordChangeOutcome.PASSWORD_NOT_SET, 0);
        }
        if (newPassword == null || newPassword.length() < MIN_PASSWORD_LENGTH) {
            return new PasswordChange(PasswordChangeOutcome.NEW_TOO_SHORT, 0);
        }
        if (newPassword.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            return new PasswordChange(PasswordChangeOutcome.NEW_TOO_LONG, 0);
        }
        if (currentPassword == null
                || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            return new PasswordChange(PasswordChangeOutcome.CURRENT_INCORRECT, 0);
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        users.save(user);
        LocalDateTime now = LocalDateTime.now(clock);
        passwordResetTokens.markAllUsedFor(user, now);
        emailVerificationTokens.markAllUsedFor(user, now);
        int revoked = devices.revokeAllExcept(user.getId(), keepTokenId);
        authEvents.record(user, user.getEmail(), AuthEventType.PASSWORD_CHANGED,
                ClientIp.fromCurrentRequest());
        return new PasswordChange(PasswordChangeOutcome.OK, revoked);
    }

    @Transactional
    public EmailChangeOutcome changeEmail(User user,
                                          String currentPassword,
                                          String newEmail) {
        if (!user.isPasswordSet()) {
            return EmailChangeOutcome.PASSWORD_NOT_SET;
        }
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
        String previousEmail = user.getEmail();
        user.setEmail(normalized);
        user.setEmailVerifiedAt(null);
        users.save(user);
        LocalDateTime now = LocalDateTime.now(clock);
        passwordResetTokens.markAllUsedFor(user, now);
        emailVerificationTokens.markAllUsedFor(user, now);
        emailVerificationService.sendVerification(user);
        String ip = ClientIp.fromCurrentRequest();
        // Two rows so the activity panel shows the change against both
        // addresses — the old one for the user who'll keep visiting from
        // muscle-memory bookmarks, the new one as the canonical record.
        authEvents.record(user, previousEmail, AuthEventType.EMAIL_CHANGED, ip);
        authEvents.record(user, normalized, AuthEventType.EMAIL_CHANGED, ip);
        return EmailChangeOutcome.OK;
    }

    private static boolean looksLikeEmail(String s) {
        int at = s.indexOf('@');
        return at > 0 && at < s.length() - 1 && s.indexOf('@', at + 1) < 0;
    }
}
