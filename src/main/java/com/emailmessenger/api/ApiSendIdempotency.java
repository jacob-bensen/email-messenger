package com.emailmessenger.api;

import com.emailmessenger.domain.ApiSendRequest;
import com.emailmessenger.repository.ApiSendRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * The {@code Idempotency-Key} store of the send endpoints (docs/API.md §5.8.1).
 * Each method is its own transaction so a claim is committed before SMTP runs:
 * the caller ({@link ApiConversationService}) is deliberately not transactional.
 */
@Service
class ApiSendIdempotency {

    static final Duration KEY_LIFETIME = Duration.ofHours(24);
    private static final Pattern KEY = Pattern.compile("[A-Za-z0-9._:-]{1,100}");

    private final ApiSendRequestRepository requests;
    private final Clock clock;

    ApiSendIdempotency(ApiSendRequestRepository requests, Clock clock) {
        this.requests = requests;
        this.clock = clock;
    }

    /** A new claim to send under ({@code replay == null}), or the stored response of a completed one. */
    record Claim(Long id, ApiDtos.SendResponse replay) {}

    static void validate(String key) {
        if (!KEY.matcher(key).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "validation_failed",
                    "Idempotency-Key must be 1–100 characters of A–Z, a–z, 0–9, '-', '_', '.' or ':'.");
        }
    }

    @Transactional
    Claim claim(long userId, String key, String requestHash) {
        LocalDateTime now = LocalDateTime.now(clock);
        requests.deleteByUserIdCreatedBefore(userId, now.minus(KEY_LIFETIME));
        var existing = requests.findByUserIdAndIdempotencyKey(userId, key);
        if (existing.isEmpty()) {
            // A concurrent claim of the same key loses on the unique constraint; the caller maps that
            // DataIntegrityViolationException to send_in_progress.
            return new Claim(requests.saveAndFlush(new ApiSendRequest(userId, key, requestHash, now)).getId(), null);
        }
        ApiSendRequest previous = existing.get();
        if (!previous.getRequestHash().equals(requestHash)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "idempotency_key_reused",
                    "This Idempotency-Key was already used for a different message.");
        }
        if (!previous.isCompleted()) {
            throw new ApiException(HttpStatus.CONFLICT, "send_in_progress",
                    "This message is still being sent, or may already have been sent.");
        }
        return new Claim(previous.getId(), new ApiDtos.SendResponse(
                previous.getConversationKey(), previous.getThreadId(), previous.getMessageId()));
    }

    @Transactional
    void complete(long claimId, ApiDtos.SendResponse response) {
        requests.findById(claimId).ifPresent(r ->
                r.complete(response.key(), response.threadId(), response.messageId(), LocalDateTime.now(clock)));
    }

    /** Forgets a claim whose send provably did not happen, so a retry sends afresh. */
    @Transactional
    void release(long claimId) {
        requests.deleteById(claimId);
    }
}
