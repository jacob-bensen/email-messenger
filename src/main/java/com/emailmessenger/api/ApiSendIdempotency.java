package com.emailmessenger.api;

import com.emailmessenger.domain.ApiSendRequest;
import com.emailmessenger.repository.ApiSendRequestRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * {@code Idempotency-Key} handling for {@code /api/v1} sends (docs/API.md
 * §5.19). Not transactional: the send it wraps must keep SMTP outside any
 * database transaction, so the claim is committed before the send runs and
 * completed (or deleted) after it.
 */
@Service
class ApiSendIdempotency {

    static final Duration KEY_LIFETIME = Duration.ofHours(24);
    static final String REPLAYED_HEADER = "Idempotent-Replayed";
    private static final Pattern KEY_FORMAT = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    record Outcome(ApiDtos.SendResponse response, boolean replayed) {}

    private final ApiSendRequestRepository requests;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    ApiSendIdempotency(ApiSendRequestRepository requests, ObjectMapper objectMapper, Clock clock) {
        this.requests = requests;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /**
     * Runs {@code send} at most once per {@code (userId, key)} within
     * {@link #KEY_LIFETIME}. {@code requestParts} identify the request; a
     * reused key with different parts is rejected. A null key just sends.
     */
    Outcome run(long userId, String key, Supplier<ApiDtos.SendResponse> send, Object... requestParts) {
        if (key == null) {
            return new Outcome(send.get(), false);
        }
        if (!KEY_FORMAT.matcher(key).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "validation_failed",
                    "Idempotency-Key must be 1–64 letters, digits, '-' or '_'.");
        }
        String fingerprint = fingerprint(requestParts);
        LocalDateTime now = LocalDateTime.now(clock);
        // Pruning here keeps the table bounded without a separate job, and
        // makes an expired row for this same key disappear before the lookup.
        requests.deleteCreatedBefore(now.minus(KEY_LIFETIME));

        Optional<ApiSendRequest> earlier = requests.findByUserIdAndIdempotencyKey(userId, key);
        if (earlier.isPresent()) {
            return replay(earlier.get(), fingerprint);
        }
        ApiSendRequest claim;
        try {
            claim = requests.saveAndFlush(new ApiSendRequest(userId, key, fingerprint, now));
        } catch (DataIntegrityViolationException raced) {
            throw inProgress();
        }

        ApiDtos.SendResponse response;
        try {
            response = send.get();
        } catch (RuntimeException e) {
            // Every failure the send flow reports happens before or instead of
            // SMTP accepting the mail, so the key is free for a retry.
            requests.delete(claim);
            throw e;
        }
        claim.complete(response.key(), response.threadId(), response.messageId(), LocalDateTime.now(clock));
        requests.save(claim);
        return new Outcome(response, false);
    }

    private static Outcome replay(ApiSendRequest earlier, String fingerprint) {
        if (!earlier.getFingerprint().equals(fingerprint)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "idempotency_key_reused",
                    "This Idempotency-Key was already used for a different message.");
        }
        if (!earlier.isCompleted()) {
            throw inProgress();
        }
        return new Outcome(new ApiDtos.SendResponse(earlier.getConversationKey(),
                earlier.getThreadId(), earlier.getMessageId()), true);
    }

    private static ApiException inProgress() {
        return new ApiException(HttpStatus.CONFLICT, "send_in_progress",
                "This message is still being sent, or may already have been sent. Refresh before trying again.");
    }

    private String fingerprint(Object... parts) {
        try {
            byte[] canonical = objectMapper.writeValueAsBytes(Arrays.asList(parts));
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical));
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Cannot fingerprint send request", e);
        }
    }
}
