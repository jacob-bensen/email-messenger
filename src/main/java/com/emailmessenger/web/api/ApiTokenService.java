package com.emailmessenger.web.api;

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
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Mints, verifies, and revokes bearer tokens used by the mobile client
 * (see {@code docs/API.md} §Auth).
 *
 * <p>The plaintext token — 32 random bytes URL-safe-base64 with a
 * {@code cmxo_} prefix — is returned to the caller once at issue time and
 * never persisted. What we store is SHA-256(plaintext) as 64-char hex, so
 * a table dump can't be replayed.
 */
@Service
public class ApiTokenService {

    static final String PLAINTEXT_PREFIX = "cmxo_";
    static final int TOKEN_BYTES = 32;

    private final ApiTokenRepository tokens;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    ApiTokenService(ApiTokenRepository tokens, Clock clock) {
        this.tokens = tokens;
        this.clock = clock;
    }

    public record IssuedToken(String plaintext, ApiToken record) {}

    @Transactional
    public IssuedToken issue(User user, String userAgent) {
        String plaintext = mintPlaintext();
        ApiToken token = new ApiToken(user, sha256Hex(plaintext), clamp(userAgent));
        return new IssuedToken(plaintext, tokens.save(token));
    }

    /**
     * Look up a still-valid token by its plaintext, bumping
     * {@code last_used_at}. Returns empty when the token is missing or
     * revoked.
     */
    @Transactional
    public Optional<ApiToken> resolve(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            return Optional.empty();
        }
        Optional<ApiToken> match = tokens.findByTokenHash(sha256Hex(plaintext));
        if (match.isEmpty()) {
            return Optional.empty();
        }
        ApiToken token = match.get();
        if (token.isRevoked()) {
            return Optional.empty();
        }
        token.setLastUsedAt(LocalDateTime.now(clock));
        return Optional.of(token);
    }

    /**
     * Soft-revoke the token that corresponds to the given plaintext.
     * Idempotent: revoking an unknown or already-revoked token is a no-op
     * so the mobile client can safely call {@code POST /api/v1/auth/logout}
     * without checking state first.
     */
    @Transactional
    public void revoke(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            return;
        }
        tokens.findByTokenHash(sha256Hex(plaintext))
                .filter(t -> !t.isRevoked())
                .ifPresent(t -> t.setRevokedAt(LocalDateTime.now(clock)));
    }

    private String mintPlaintext() {
        byte[] buf = new byte[TOKEN_BYTES];
        random.nextBytes(buf);
        return PLAINTEXT_PREFIX
                + Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private static String clamp(String ua) {
        if (ua == null) {
            return null;
        }
        String trimmed = ua.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() > 255 ? trimmed.substring(0, 255) : trimmed;
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
