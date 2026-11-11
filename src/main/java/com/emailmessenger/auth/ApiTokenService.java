package com.emailmessenger.auth;

import com.emailmessenger.domain.ApiToken;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.ApiTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

/**
 * Issues, resolves, and revokes the opaque bearer tokens the mobile client
 * presents on {@code /api/v1/**}. The plain token leaves the server exactly
 * once (in the login response); only its SHA-256 is stored.
 */
@Service
public class ApiTokenService {

    public static final Duration TOKEN_TTL = Duration.ofDays(90);
    static final String PREFIX = "cxm_";
    static final int TOKEN_BYTES = 32;
    static final int MAX_DEVICE_NAME = 100;
    // last_used_at is informational; don't turn every API read into a write.
    static final Duration LAST_USED_GRANULARITY = Duration.ofMinutes(5);

    public record IssuedToken(String token, LocalDateTime expiresAt) {}

    private final ApiTokenRepository tokens;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    ApiTokenService(ApiTokenRepository tokens, Clock clock) {
        this.tokens = tokens;
        this.clock = clock;
    }

    @Transactional
    public IssuedToken issue(User user, String deviceName) {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String plain = PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime expiresAt = now.plus(TOKEN_TTL);
        tokens.save(new ApiToken(user, PasswordResetService.sha256Hex(plain),
                normalizeDeviceName(deviceName), now, expiresAt));
        return new IssuedToken(plain, expiresAt);
    }

    /** The user a presented token belongs to, or empty when it is unknown, expired, revoked, or the user is disabled. */
    @Transactional
    public Optional<User> authenticate(String plainToken) {
        return findLive(plainToken).map(token -> {
            LocalDateTime now = LocalDateTime.now(clock);
            LocalDateTime lastUsed = token.getLastUsedAt();
            if (lastUsed == null || lastUsed.plus(LAST_USED_GRANULARITY).isBefore(now)) {
                token.setLastUsedAt(now);
            }
            return token.getUser();
        });
    }

    /** Revokes one token (logout on that device). Unknown or dead tokens are a no-op. */
    @Transactional
    public void revoke(String plainToken) {
        findLive(plainToken).ifPresent(token -> token.setRevokedAt(LocalDateTime.now(clock)));
    }

    /** Signs every device out — called whenever the user's password changes. */
    @Transactional
    public void revokeAllFor(User user) {
        tokens.revokeAllFor(user, LocalDateTime.now(clock));
    }

    private Optional<ApiToken> findLive(String plainToken) {
        if (plainToken == null || !plainToken.startsWith(PREFIX)) {
            return Optional.empty();
        }
        LocalDateTime now = LocalDateTime.now(clock);
        return tokens.findByTokenHash(PasswordResetService.sha256Hex(plainToken))
                .filter(token -> token.isLive(now))
                .filter(token -> token.getUser().isEnabled());
    }

    private static String normalizeDeviceName(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.length() > MAX_DEVICE_NAME ? trimmed.substring(0, MAX_DEVICE_NAME) : trimmed;
    }
}
