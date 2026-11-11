package com.emailmessenger.auth;

import com.emailmessenger.domain.ApiToken;
import com.emailmessenger.domain.AuthEventType;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.ApiTokenRepository;
import com.emailmessenger.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;

/**
 * Password login for the mobile REST API. Issues opaque bearer tokens
 * (stored hashed) and resolves them back to a user on each request. Shares
 * the web login's throttle and audit trail: failures land in auth_events, so
 * API and form-login attempts count toward the same lockout window.
 */
@Service
public class ApiTokenService {

    private static final String BEARER_PREFIX = "Bearer ";
    static final Duration TOKEN_TTL = Duration.ofDays(90);

    public record IssuedToken(String token, Instant expiresAt, User user) {}

    public static class InvalidCredentialsException extends RuntimeException {}

    public static class LoginThrottledException extends RuntimeException {}

    private final ApiTokenRepository tokens;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final LoginThrottleService throttle;
    private final AuthEventService authEvents;
    private final UserActivityService activity;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    ApiTokenService(ApiTokenRepository tokens,
                    UserRepository users,
                    PasswordEncoder passwordEncoder,
                    LoginThrottleService throttle,
                    AuthEventService authEvents,
                    UserActivityService activity,
                    Clock clock) {
        this.tokens = tokens;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.throttle = throttle;
        this.authEvents = authEvents;
        this.activity = activity;
        this.clock = clock;
    }

    // noRollbackFor: the LOGIN_FAILURE audit row must survive the rejection,
    // otherwise API failures would never count toward the lockout.
    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public IssuedToken login(String email, String rawPassword, String ipAddress) {
        String normalized = UserService.normalizeEmail(email);
        if (throttle.isLocked(normalized, ipAddress)) {
            throw new LoginThrottledException();
        }
        User user = users.findByEmail(normalized).orElse(null);
        // Google-only accounts carry a random unusable hash; passwordSet gates them out.
        boolean ok = user != null && user.isEnabled() && user.isPasswordSet()
                && rawPassword != null
                && passwordEncoder.matches(rawPassword, user.getPasswordHash());
        if (!ok) {
            authEvents.record(user, normalized, AuthEventType.LOGIN_FAILURE, ipAddress);
            if (throttle.isLocked(normalized, ipAddress)) {
                authEvents.record(user, normalized, AuthEventType.ACCOUNT_LOCKED, ipAddress);
            }
            throw new InvalidCredentialsException();
        }

        byte[] buf = new byte[32];
        random.nextBytes(buf);
        String plain = Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime expiresAt = now.plus(TOKEN_TTL);
        tokens.save(new ApiToken(user, PasswordResetService.sha256Hex(plain), now, expiresAt));

        activity.recordLogin(user.getEmail());
        authEvents.record(user, normalized, AuthEventType.LOGIN_SUCCESS, ipAddress);
        return new IssuedToken(plain, expiresAt.toInstant(ZoneOffset.UTC), user);
    }

    /** The email of the enabled user this token belongs to, if it is still active. */
    @Transactional(readOnly = true)
    public Optional<String> resolveEmail(String plainToken) {
        if (plainToken == null || plainToken.isBlank()) {
            return Optional.empty();
        }
        return tokens.findByTokenHash(PasswordResetService.sha256Hex(plainToken))
                .filter(t -> t.isActive(LocalDateTime.now(clock)))
                .map(ApiToken::getUser)
                .filter(User::isEnabled)
                .map(User::getEmail);
    }

    @Transactional
    public void revoke(String plainToken) {
        if (plainToken == null || plainToken.isBlank()) {
            return;
        }
        tokens.findByTokenHash(PasswordResetService.sha256Hex(plainToken))
                .ifPresent(t -> t.revoke(LocalDateTime.now(clock)));
    }

    /** The token from an {@code Authorization: Bearer} header, or null. */
    public static String bearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
