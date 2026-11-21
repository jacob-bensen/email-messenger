package com.emailmessenger.api;

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
 * Issues and checks the opaque bearer tokens of {@code /api/v1}. The raw token
 * is returned once at sign-in and never stored; lookups go by its SHA-256.
 */
@Service
class ApiTokenService {

    static final Duration TOKEN_LIFETIME = Duration.ofDays(30);
    static final int MAX_DEVICE_NAME_LENGTH = 100;

    // Writing last_used_at on every request would turn each read into a write;
    // once per few minutes is enough to show "last active" per device.
    private static final Duration LAST_USED_GRANULARITY = Duration.ofMinutes(5);

    private final ApiTokenRepository tokens;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    ApiTokenService(ApiTokenRepository tokens, Clock clock) {
        this.tokens = tokens;
        this.clock = clock;
    }

    record IssuedToken(String rawToken, LocalDateTime expiresAt) {}

    @Transactional
    IssuedToken issue(User user, String deviceName) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime expiresAt = now.plus(TOKEN_LIFETIME);
        tokens.save(new ApiToken(user, hash(raw), trimDeviceName(deviceName), now, expiresAt));
        return new IssuedToken(raw, expiresAt);
    }

    /** The principal for a valid, unexpired token whose user is still enabled. */
    @Transactional
    Optional<ApiPrincipal> authenticate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        LocalDateTime now = LocalDateTime.now(clock);
        return tokens.findWithUserByTokenHash(hash(rawToken))
                .filter(token -> !token.isExpired(now))
                .filter(token -> token.getUser().isEnabled())
                .map(token -> {
                    LocalDateTime last = token.getLastUsedAt();
                    if (last == null || last.plus(LAST_USED_GRANULARITY).isBefore(now)) {
                        token.setLastUsedAt(now);
                    }
                    return new ApiPrincipal(token.getUser().getId(), token.getUser().getEmail(), token.getId());
                });
    }

    /** Whether a token id still authenticates: not revoked, unexpired, user enabled. */
    @Transactional(readOnly = true)
    boolean isActive(long tokenId) {
        LocalDateTime now = LocalDateTime.now(clock);
        return tokens.findById(tokenId)
                .filter(token -> !token.isExpired(now))
                .filter(token -> token.getUser().isEnabled())
                .isPresent();
    }

    @Transactional
    void revoke(long tokenId) {
        tokens.deleteById(tokenId);
    }

    static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required but unavailable", e);
        }
    }

    private static String trimDeviceName(String deviceName) {
        if (deviceName == null || deviceName.isBlank()) {
            return null;
        }
        String trimmed = deviceName.trim();
        return trimmed.length() > MAX_DEVICE_NAME_LENGTH ? trimmed.substring(0, MAX_DEVICE_NAME_LENGTH) : trimmed;
    }
}
