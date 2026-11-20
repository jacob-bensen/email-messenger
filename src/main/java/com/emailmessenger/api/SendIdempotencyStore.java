package com.emailmessenger.api;

import com.emailmessenger.domain.ApiSendRequest;
import com.emailmessenger.repository.ApiSendRequestRepository;
import com.emailmessenger.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Persistence for docs/API.md §5.8.1. Each method is its own short transaction so SMTP, which
 * runs between {@link #reserve} and {@link #complete}, stays outside any database transaction.
 */
@Service
class SendIdempotencyStore {

    static final Duration RETENTION = Duration.ofHours(24);

    /** An earlier request with the same key: its fingerprint and, once finished, its response. */
    record Prior(String requestHash, ApiDtos.SendResponse response) {}

    private final ApiSendRequestRepository requests;
    private final UserRepository users;
    private final Clock clock;

    SendIdempotencyStore(ApiSendRequestRepository requests, UserRepository users, Clock clock) {
        this.requests = requests;
        this.users = users;
        this.clock = clock;
    }

    /**
     * Claims {@code key} for a new send and returns {@code null}, or returns the unexpired earlier
     * request that holds it. A concurrent claim of the same key fails the unique constraint with a
     * {@link org.springframework.dao.DataIntegrityViolationException}.
     */
    @Transactional
    Prior reserve(long userId, String key, String requestHash) {
        LocalDateTime now = LocalDateTime.now(clock);
        var existing = requests.findByUserIdAndKey(userId, key);
        if (existing.isPresent()) {
            ApiSendRequest prior = existing.get();
            if (prior.getCreatedAt().isAfter(now.minus(RETENTION))) {
                return new Prior(prior.getRequestHash(), prior.isCompleted()
                        ? new ApiDtos.SendResponse(prior.getConversationKey(), prior.getThreadId(), prior.getMessageId())
                        : null);
            }
            requests.deleteByUserIdAndKey(userId, key);
        }
        requests.saveAndFlush(new ApiSendRequest(users.getReferenceById(userId), key, requestHash, now));
        return null;
    }

    @Transactional
    void complete(long userId, String key, ApiDtos.SendResponse response) {
        requests.findByUserIdAndKey(userId, key).ifPresent(r ->
                r.complete(response.key(), response.threadId(), response.messageId(), LocalDateTime.now(clock)));
    }

    /** Forgets a key whose send was rejected before anything went out, so a retry sends normally. */
    @Transactional
    void release(long userId, String key) {
        requests.deleteByUserIdAndKey(userId, key);
    }
}
