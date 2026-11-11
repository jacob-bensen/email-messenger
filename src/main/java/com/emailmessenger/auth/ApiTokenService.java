package com.emailmessenger.auth;

import com.emailmessenger.domain.ApiToken;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.ApiTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
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
 * Issues, resolves, and revokes the opaque bearer tokens the mobile client
 * sends to {@code /api/v1/**}. The raw token exists only in the login
 * response; the database keeps its SHA-256 (the token is 256 bits of
 * {@link SecureRandom} output, so an unsalted fast hash is sufficient).
 */
@Service
public class ApiTokenService {

    static final String PREFIX = "cmx_";
    private static final String BEARER = "Bearer ";
    private static final int DEVICE_NAME_MAX = 100;
    // last_used_at is informational; skip the write when it is this fresh so
    // a burst of API calls doesn't turn every read into an UPDATE.
    private static final Duration LAST_USED_GRANULARITY = Duration.ofMinutes(5);

    public record IssuedToken(String token, LocalDateTime expiresAt) {}

    private final ApiTokenRepository tokens;
    private final Clock clock;
    private final Duration ttl;
    private final SecureRandom random = new SecureRandom();

    ApiTokenService(ApiTokenRepository tokens,
                    Clock clock,
                    @Value("${api.token.ttl-days:90}") int ttlDays) {
        this.tokens = tokens;
        this.clock = clock;
        this.ttl = Duration.ofDays(Math.max(1, ttlDays));
    }

    @Transactional
    public IssuedToken issue(User user, String deviceName) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime expiresAt = now.plus(ttl);
        tokens.save(new ApiToken(user, hash(raw), cleanDeviceName(deviceName), now, expiresAt));
        return new IssuedToken(raw, expiresAt);
    }

    /** The enabled user a live (unexpired, unrevoked) token belongs to. */
    @Transactional
    public Optional<User> authenticate(String rawToken) {
        if (rawToken == null || !rawToken.startsWith(PREFIX)) {
            return Optional.empty();
        }
        LocalDateTime now = LocalDateTime.now(clock);
        return tokens.findByTokenHashWithUser(hash(rawToken))
                .filter(t -> t.isUsableAt(now) && t.getUser().isEnabled())
                .map(t -> {
                    if (t.getLastUsedAt() == null
                            || t.getLastUsedAt().isBefore(now.minus(LAST_USED_GRANULARITY))) {
                        t.setLastUsedAt(now);
                    }
                    return t.getUser();
                });
    }

    /** Idempotent: unknown or already-revoked tokens are ignored. */
    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        tokens.findByTokenHashWithUser(hash(rawToken))
                .filter(t -> t.getRevokedAt() == null)
                .ifPresent(t -> t.setRevokedAt(LocalDateTime.now(clock)));
    }

    /**
     * Signs the user out of every device. Called whenever the password
     * changes, so a credential the user has just replaced can't leave a
     * previously issued token alive.
     */
    @Transactional
    public void revokeAllFor(User user) {
        LocalDateTime now = LocalDateTime.now(clock);
        for (ApiToken token : tokens.findByUserAndRevokedAtIsNull(user)) {
            token.setRevokedAt(now);
        }
    }

    /** The token in {@code Authorization: Bearer <token>}, or {@code null}. */
    public static String bearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
            return null;
        }
        String token = header.substring(BEARER.length()).trim();
        return token.isEmpty() ? null : token;
    }

    private static String cleanDeviceName(String deviceName) {
        if (deviceName == null || deviceName.isBlank()) {
            return null;
        }
        String trimmed = deviceName.trim();
        return trimmed.length() > DEVICE_NAME_MAX ? trimmed.substring(0, DEVICE_NAME_MAX) : trimmed;
    }

    static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
