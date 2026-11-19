package com.emailmessenger.auth;

import com.emailmessenger.repository.ApiTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * The user's signed-in apps (API tokens) and signing them out. Shared by the
 * website's /account page and {@code /api/v1/account/devices}; every query is
 * scoped to the owner, so a foreign token id behaves like an unknown one.
 */
@Service
public class SignedInDeviceService {

    private final ApiTokenRepository tokens;
    private final Clock clock;

    SignedInDeviceService(ApiTokenRepository tokens, Clock clock) {
        this.tokens = tokens;
        this.clock = clock;
    }

    /** Unexpired tokens, most recently used first. {@code currentTokenId} is null on the website. */
    @Transactional(readOnly = true)
    public List<SignedInDevice> list(long userId, Long currentTokenId) {
        return tokens.findActiveByUserId(userId, LocalDateTime.now(clock)).stream()
                .map(t -> new SignedInDevice(t.getId(), t.getDeviceName(), t.getCreatedAt(),
                        t.getLastUsedAt(), t.getExpiresAt(), t.getId().equals(currentTokenId)))
                .toList();
    }

    /** False when the token is unknown, expired, already revoked or not the user's. */
    @Transactional
    public boolean revoke(long userId, long tokenId) {
        return tokens.deleteActiveByIdAndUserId(tokenId, userId, LocalDateTime.now(clock)) > 0;
    }

    /** Revokes the user's unexpired tokens except {@code keepTokenId} (null keeps none); returns how many. */
    @Transactional
    public int revokeAllExcept(long userId, Long keepTokenId) {
        LocalDateTime now = LocalDateTime.now(clock);
        return keepTokenId == null
                ? tokens.deleteActiveByUserId(userId, now)
                : tokens.deleteActiveByUserIdExcept(userId, keepTokenId, now);
    }
}
