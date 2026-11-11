package com.emailmessenger.auth;

import com.emailmessenger.domain.ApiToken;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.ApiTokenRepository;
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
 * Opaque bearer tokens for the mobile REST API. The plaintext (32 random
 * bytes, URL-safe base64) is handed to the client once at login; only its
 * SHA-256 hex is stored, the same scheme {@link PasswordResetService} uses.
 * Tokens live for {@link #TOKEN_TTL} and die early on logout or when the
 * user's password changes.
 */
@Service
public class ApiTokenService {

    static final Duration TOKEN_TTL = Duration.ofDays(90);
    static final int TOKEN_BYTES = 32;
    static final int MAX_DEVICE_NAME = 100;
    // last_used_at is informational; don't turn every API read into a write.
    private static final Duration LAST_USED_GRANULARITY = Duration.ofMinutes(15);

    public record IssuedToken(String plainToken, LocalDateTime expiresAt) {}

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
        String plain = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime expiresAt = now.plus(TOKEN_TTL);
        tokens.save(new ApiToken(user, sha256Hex(plain), cleanDeviceName(deviceName), now, expiresAt));
        return new IssuedToken(plain, expiresAt);
    }

    /**
     * The enabled user a live token belongs to, or empty for an unknown,
     * expired, or revoked token, or one whose user has been disabled.
     */
    @Transactional
    public Optional<User> authenticate(String plainToken) {
        if (plainToken == null || plainToken.isBlank()) {
            return Optional.empty();
        }
        Optional<ApiToken> match = tokens.findByTokenHashWithUser(sha256Hex(plainToken));
        if (match.isEmpty()) {
            return Optional.empty();
        }
        ApiToken token = match.get();
        LocalDateTime now = LocalDateTime.now(clock);
        if (!token.isUsable(now) || !token.getUser().isEnabled()) {
            return Optional.empty();
        }
        if (token.getLastUsedAt() == null
                || token.getLastUsedAt().isBefore(now.minus(LAST_USED_GRANULARITY))) {
            token.setLastUsedAt(now);
        }
        return Optional.of(token.getUser());
    }

    /** Revokes one token (logout). Unknown or already-revoked tokens are a no-op. */
    @Transactional
    public void revoke(String plainToken) {
        if (plainToken == null || plainToken.isBlank()) {
            return;
        }
        tokens.findByTokenHashWithUser(sha256Hex(plainToken)).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(LocalDateTime.now(clock));
            }
        });
    }

    /** Signs every device out — called when the user's password changes. */
    @Transactional
    public int revokeAllFor(User user) {
        return tokens.revokeAllFor(user, LocalDateTime.now(clock));
    }

    private static String cleanDeviceName(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.length() > MAX_DEVICE_NAME ? trimmed.substring(0, MAX_DEVICE_NAME) : trimmed;
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
