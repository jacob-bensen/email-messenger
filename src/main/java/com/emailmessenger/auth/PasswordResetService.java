package com.emailmessenger.auth;

import com.emailmessenger.domain.AuthEventType;
import com.emailmessenger.domain.PasswordResetToken;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.PasswordResetTokenRepository;
import com.emailmessenger.repository.UserRepository;
import com.emailmessenger.web.SiteProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Password reset via emailed token.
 *
 * <p>Flow:
 * <ol>
 *   <li>{@link #requestReset(String)} — looks up the user by email and,
 *       if found, mints a fresh token, stores its SHA-256 hash, and
 *       emails a reset URL. Unknown emails are silently ignored so the
 *       endpoint can't be used for account enumeration; the caller
 *       always shows the same "if we know that email, you'll get a link"
 *       confirmation screen.</li>
 *   <li>{@link #consumeToken(String, String)} — validates the token
 *       (exists, not used, not expired), updates the user's password
 *       hash, marks every outstanding token for that user as used so a
 *       race with another reset email can't yield a second login
 *       window, and returns the affected {@link User}.</li>
 * </ol>
 *
 * <p>The plaintext token (32 random bytes URL-safe base64) only exists
 * in the email body and in the URL the user clicks; the DB stores
 * SHA-256(token) hex so a table dump can't be replayed. Tokens are
 * single-use and expire in 1 hour.
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    static final Duration TOKEN_TTL = Duration.ofHours(1);
    static final int TOKEN_BYTES = 32;

    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final SiteProperties site;
    private final AuthEventService authEvents;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    @Value("${spring.mail.username:noreply@mailaim.app}")
    private String fromAddress = "noreply@mailaim.app";

    PasswordResetService(UserRepository users,
                         PasswordResetTokenRepository tokens,
                         PasswordEncoder passwordEncoder,
                         JavaMailSender mailSender,
                         SiteProperties site,
                         AuthEventService authEvents,
                         Clock clock) {
        this.users = users;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.site = site;
        this.authEvents = authEvents;
        this.clock = clock;
    }

    /**
     * Outcome of {@link #requestReset(String)}. Callers map this to UI:
     * {@link #SENT} and {@link #IGNORED} both render the generic
     * "if we know it, you'll get a link" confirmation so the response
     * can't enumerate registered addresses, while {@link #GOOGLE_ONLY}
     * intentionally tells the visitor to use "Continue with Google"
     * instead — letting a Google-provisioned user reset to a chosen
     * password would silently mint a credential they could later use to
     * bypass Google entirely, which is the opposite of what
     * single-sign-on offered.
     */
    public enum Outcome { SENT, IGNORED, GOOGLE_ONLY }

    /**
     * Issue a reset email when we know the address and the user has a
     * password to reset; return {@link Outcome#GOOGLE_ONLY} for a
     * Google-provisioned user (no email sent — they have no password to
     * reset); {@link Outcome#IGNORED} for an unknown / disabled row /
     * mail send failure. Callers must not branch the generic
     * confirmation UI on {@link Outcome#SENT} vs {@link Outcome#IGNORED}
     * — that would leak which emails are registered — but
     * {@link Outcome#GOOGLE_ONLY} drives a deliberately distinct UI.
     */
    @Transactional
    public Outcome requestReset(String email) {
        String normalized = UserService.normalizeEmail(email);
        if (normalized == null || normalized.isEmpty()) {
            return Outcome.IGNORED;
        }
        Optional<User> match = users.findByEmail(normalized);
        if (match.isEmpty()) {
            return Outcome.IGNORED;
        }
        User user = match.get();
        if (!user.isEnabled()) {
            return Outcome.IGNORED;
        }
        if (user.isGoogleOnly()) {
            return Outcome.GOOGLE_ONLY;
        }
        String plain = newPlainToken();
        LocalDateTime now = LocalDateTime.now(clock);
        PasswordResetToken token = new PasswordResetToken(
                user, sha256Hex(plain), now.plus(TOKEN_TTL));
        tokens.save(token);
        try {
            mailSender.send(compose(user, plain));
        } catch (MailException e) {
            log.warn("Password-reset mail send failed for user id={}: {}",
                    user.getId(), e.getMessage());
            return Outcome.IGNORED;
        }
        return Outcome.SENT;
    }

    /**
     * Look up a token and return the owning user when the token is
     * still valid (exists, not used, not expired). Used by the GET form
     * to fail fast before the user types a new password into a dead
     * link.
     */
    @Transactional(readOnly = true)
    public Optional<User> findUserForValidToken(String plainToken) {
        if (plainToken == null || plainToken.isBlank()) {
            return Optional.empty();
        }
        Optional<PasswordResetToken> match = tokens.findByTokenHash(sha256Hex(plainToken));
        if (match.isEmpty()) {
            return Optional.empty();
        }
        PasswordResetToken token = match.get();
        if (token.isUsed() || token.isExpired(LocalDateTime.now(clock))) {
            return Optional.empty();
        }
        return Optional.of(token.getUser());
    }

    /**
     * Consume the token, set the new password, and revoke every other
     * outstanding token for the same user. Returns the affected user on
     * success, empty when the token was missing / used / expired.
     */
    @Transactional
    public Optional<User> consumeToken(String plainToken, String newPassword) {
        if (plainToken == null || plainToken.isBlank()
                || newPassword == null || newPassword.length() < 8) {
            return Optional.empty();
        }
        Optional<PasswordResetToken> match = tokens.findByTokenHash(sha256Hex(plainToken));
        if (match.isEmpty()) {
            return Optional.empty();
        }
        PasswordResetToken token = match.get();
        LocalDateTime now = LocalDateTime.now(clock);
        if (token.isUsed() || token.isExpired(now)) {
            return Optional.empty();
        }
        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        // The user just picked this password, so they're no longer
        // Google-only — future /password/forgot requests should work.
        user.setPasswordSet(true);
        users.save(user);
        tokens.markAllUsedFor(user, now);
        authEvents.record(user, user.getEmail(), AuthEventType.PASSWORD_RESET_COMPLETED,
                ClientIp.fromCurrentRequest());
        return Optional.of(user);
    }

    private MimeMessage compose(User user, String plainToken) {
        MimeMessage mime = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(user.getEmail());
            helper.setSubject("Reset your MailIM password");
            helper.setText(renderBody(user, plainToken), false);
        } catch (MessagingException e) {
            throw new MailPreparationException("Could not compose password-reset email", e);
        }
        return mime;
    }

    private String renderBody(User user, String plainToken) {
        String greeting = (user.getDisplayName() != null && !user.getDisplayName().isBlank())
                ? user.getDisplayName().trim() : "there";
        String url = site.getBaseUrl() + "/password/reset?token=" + plainToken;
        return "Hi " + greeting + ",\n\n"
                + "Someone (hopefully you) asked to reset the password on your "
                + "MailIM account.\n\n"
                + "Click the link below within the next hour to set a new "
                + "password:\n\n"
                + url + "\n\n"
                + "If you didn't request this, you can ignore this email — your "
                + "password won't change.\n";
    }

    private String newPlainToken() {
        byte[] buf = new byte[TOKEN_BYTES];
        random.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
