package com.emailmessenger.auth;

import com.emailmessenger.domain.EmailVerificationToken;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.EmailVerificationTokenRepository;
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
 * Email verification on signup.
 *
 * <p>{@link #sendVerification(User)} is the entry point: mints a fresh
 * token, stores its SHA-256 hash, and emails the user a confirmation
 * URL. The plaintext token (32 random bytes URL-safe base64) only ever
 * exists in the email body / URL the user clicks; the DB stores
 * SHA-256(token) hex so a table dump can't be replayed.
 *
 * <p>{@link #verify(String)} consumes a token, sets
 * {@code users.email_verified_at}, and marks every outstanding token
 * for the same user as used so a stale verification email can't be
 * replayed after the address is already confirmed.
 *
 * <p>Mail-send failures are logged and swallowed — registration must
 * not fail if the SMTP relay flakes; the user can re-request a
 * verification link from the unverified-account banner on /threads.
 */
@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);

    static final Duration TOKEN_TTL = Duration.ofHours(24);
    static final int TOKEN_BYTES = 32;

    private final UserRepository users;
    private final EmailVerificationTokenRepository tokens;
    private final JavaMailSender mailSender;
    private final SiteProperties site;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    @Value("${spring.mail.username:noreply@mailaim.app}")
    private String fromAddress = "noreply@mailaim.app";

    EmailVerificationService(UserRepository users,
                             EmailVerificationTokenRepository tokens,
                             JavaMailSender mailSender,
                             SiteProperties site,
                             Clock clock) {
        this.users = users;
        this.tokens = tokens;
        this.mailSender = mailSender;
        this.site = site;
        this.clock = clock;
    }

    /**
     * Issue a verification email for the given user. No-op (and silent)
     * if the address is already verified, so a duplicate resend doesn't
     * mint a second live token after the first has already been
     * consumed. Returns true when a fresh token was minted and the mail
     * accepted.
     */
    @Transactional
    public boolean sendVerification(User user) {
        if (user == null || user.isEmailVerified()) {
            return false;
        }
        String plain = newPlainToken();
        LocalDateTime now = LocalDateTime.now(clock);
        EmailVerificationToken token = new EmailVerificationToken(
                user, sha256Hex(plain), now.plus(TOKEN_TTL));
        tokens.save(token);
        try {
            mailSender.send(compose(user, plain));
        } catch (MailException e) {
            log.warn("Verification mail send failed for user id={}: {}",
                    user.getId(), e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Look up a token and, if it's still valid, set the user's
     * {@code email_verified_at} and revoke every outstanding token.
     * Returns the verified user on success, empty when the token was
     * missing / used / expired.
     */
    @Transactional
    public Optional<User> verify(String plainToken) {
        if (plainToken == null || plainToken.isBlank()) {
            return Optional.empty();
        }
        Optional<EmailVerificationToken> match = tokens.findByTokenHash(sha256Hex(plainToken));
        if (match.isEmpty()) {
            return Optional.empty();
        }
        EmailVerificationToken token = match.get();
        LocalDateTime now = LocalDateTime.now(clock);
        if (token.isUsed() || token.isExpired(now)) {
            return Optional.empty();
        }
        User user = token.getUser();
        if (user.getEmailVerifiedAt() == null) {
            user.setEmailVerifiedAt(now);
            users.save(user);
        }
        tokens.markAllUsedFor(user, now);
        return Optional.of(user);
    }

    private MimeMessage compose(User user, String plainToken) {
        MimeMessage mime = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(user.getEmail());
            helper.setSubject("Confirm your MailIM email address");
            helper.setText(renderBody(user, plainToken), false);
        } catch (MessagingException e) {
            throw new MailPreparationException("Could not compose verification email", e);
        }
        return mime;
    }

    private String renderBody(User user, String plainToken) {
        String greeting = (user.getDisplayName() != null && !user.getDisplayName().isBlank())
                ? user.getDisplayName().trim() : "there";
        String url = site.getBaseUrl() + "/verify-email?token=" + plainToken;
        return "Hi " + greeting + ",\n\n"
                + "Welcome to MailIM. Click the link below within the next 24 "
                + "hours to confirm your email address:\n\n"
                + url + "\n\n"
                + "If you didn't sign up for MailIM, you can ignore this "
                + "email — we won't bother you again.\n";
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
