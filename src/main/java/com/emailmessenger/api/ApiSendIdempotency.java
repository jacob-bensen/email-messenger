package com.emailmessenger.api;

import com.emailmessenger.domain.ApiSendRequest;
import com.emailmessenger.repository.ApiSendRequestRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * docs/API.md §5.8.1. Each step is its own short transaction so the SMTP send in between runs
 * outside any database transaction, as {@link com.emailmessenger.web.ChatSendService} requires.
 */
@Service
class ApiSendIdempotency {

    static final Duration RETENTION = Duration.ofHours(24);
    private static final Pattern VALID_KEY = Pattern.compile("[\\x21-\\x7E]{1,100}");

    /** Outcome of {@link #claim}: either a stored response to replay, or a claim to finish. */
    record Claim(Long requestId, ApiDtos.SendResponse replay) {}

    private final ApiSendRequestRepository requests;
    private final Clock clock;

    ApiSendIdempotency(ApiSendRequestRepository requests, Clock clock) {
        this.requests = requests;
        this.clock = clock;
    }

    static void validate(String key) {
        if (key != null && !VALID_KEY.matcher(key).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "validation_failed",
                    "Idempotency-Key must be 1–100 printable ASCII characters.");
        }
    }

    /**
     * Length-prefixes every part so no two different requests can join to the same string
     * (e.g. a subject ending where the body begins).
     */
    static String fingerprint(Object... parts) {
        StringBuilder canonical = new StringBuilder();
        for (Object part : parts) {
            String s = part == null ? "\u0000" : part.toString();
            canonical.append(s.length()).append(':').append(s).append('|');
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Transactional
    Claim claim(long userId, String key, String fingerprint) {
        LocalDateTime now = LocalDateTime.now(clock);
        Optional<ApiSendRequest> existing = requests.findByUserIdAndIdempotencyKey(userId, key);
        if (existing.isPresent()) {
            ApiSendRequest seen = existing.get();
            if (seen.getCreatedAt().isAfter(now.minus(RETENTION))) {
                return replayOrRefuse(seen, fingerprint);
            }
            requests.delete(seen);
            requests.flush();
        }
        try {
            return new Claim(requests.saveAndFlush(new ApiSendRequest(userId, key, fingerprint, now)).getId(), null);
        } catch (DataIntegrityViolationException raced) {
            // A concurrent request with the same key won the insert; it is still running.
            throw inProgress();
        }
    }

    @Transactional
    void complete(long requestId, ApiDtos.SendResponse response) {
        requests.findById(requestId)
                .ifPresent(r -> r.complete(response.key(), response.threadId(), response.messageId()));
    }

    /** Frees the key after a send that provably sent nothing. */
    @Transactional
    void release(long requestId) {
        requests.deleteById(requestId);
    }

    private static Claim replayOrRefuse(ApiSendRequest seen, String fingerprint) {
        if (!seen.getFingerprint().equals(fingerprint)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "idempotency_key_reused",
                    "This Idempotency-Key was already used for a different message.");
        }
        if (seen.getStatus() == ApiSendRequest.Status.DONE) {
            return new Claim(null, new ApiDtos.SendResponse(seen.getConversationKey(),
                    seen.getThreadId(), seen.getMessageId()));
        }
        throw inProgress();
    }

    private static ApiException inProgress() {
        return new ApiException(HttpStatus.CONFLICT, "send_in_progress",
                "This message may already be on its way. Check the conversation before sending it again.");
    }
}
