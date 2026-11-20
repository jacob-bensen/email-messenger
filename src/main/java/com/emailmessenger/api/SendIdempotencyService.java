package com.emailmessenger.api;

import com.emailmessenger.domain.ApiSendRequest;
import com.emailmessenger.repository.ApiSendRequestRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * The {@code Idempotency-Key} rule of docs/API.md §5.8: a key is claimed before
 * the SMTP send, completed with the recorded message after it, and released if
 * the send fails, so the client can retry a timed-out send without sending twice.
 */
@Service
class SendIdempotencyService {

    static final Duration RETENTION = Duration.ofHours(24);
    // A claim this old belongs to a request that died mid-send (e.g. a restart);
    // the SMTP timeouts are far shorter, so taking it over cannot race a live send.
    static final Duration STALE_CLAIM = Duration.ofMinutes(10);

    private static final Pattern KEY = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private final ApiSendRequestRepository requests;
    private final Clock clock;
    private final TransactionTemplate tx;

    SendIdempotencyService(ApiSendRequestRepository requests, Clock clock, PlatformTransactionManager txManager) {
        this.requests = requests;
        this.clock = clock;
        this.tx = new TransactionTemplate(txManager);
    }

    /**
     * Runs {@code send} at most once per (user, key). With no key it just sends.
     * Not transactional: each step commits on its own, so the claim is visible to
     * a concurrent retry while SMTP runs.
     */
    ApiDtos.SendResponse once(long userId, String key, String requestHash, Supplier<ApiDtos.SendResponse> send) {
        if (key == null) {
            return send.get();
        }
        if (!KEY.matcher(key).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "validation_failed",
                    "Idempotency-Key must be 1–64 letters, digits, '-' or '_'.");
        }
        Claim claim = claim(userId, key, requestHash);
        if (claim.replay() != null) {
            return claim.replay();
        }
        ApiDtos.SendResponse response;
        try {
            response = send.get();
        } catch (RuntimeException e) {
            tx.executeWithoutResult(status -> requests.deletePending(claim.id()));
            throw e;
        }
        tx.executeWithoutResult(status -> complete(claim.id(), response));
        return response;
    }

    private record Claim(Long id, ApiDtos.SendResponse replay) {}

    private Claim claim(long userId, String key, String requestHash) {
        try {
            return tx.execute(status -> tryClaim(userId, key, requestHash));
        } catch (DataIntegrityViolationException e) {
            // A concurrent request inserted the same key between our read and insert.
            return tx.execute(status -> tryClaim(userId, key, requestHash));
        }
    }

    private Claim tryClaim(long userId, String key, String requestHash) {
        LocalDateTime now = LocalDateTime.now(clock);
        Optional<ApiSendRequest> existing = requests.findByUserIdAndIdempotencyKey(userId, key);
        if (existing.isEmpty()) {
            return new Claim(requests.saveAndFlush(new ApiSendRequest(userId, key, requestHash, now)).getId(), null);
        }
        ApiSendRequest row = existing.get();
        boolean expired = row.getClaimedAt().isBefore(now.minus(RETENTION));
        boolean stale = !row.isCompleted() && row.getClaimedAt().isBefore(now.minus(STALE_CLAIM));
        if (expired) {
            requests.delete(row);
            requests.flush();
            return new Claim(requests.saveAndFlush(new ApiSendRequest(userId, key, requestHash, now)).getId(), null);
        }
        if (!row.getRequestHash().equals(requestHash)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "idempotency_key_reused",
                    "This Idempotency-Key was already used for a different message.");
        }
        if (row.isCompleted()) {
            return new Claim(row.getId(), new ApiDtos.SendResponse(
                    row.getConversationKey(), row.getThreadId(), row.getMessageId()));
        }
        if (stale && requests.reclaimStale(row.getId(), now.minus(STALE_CLAIM), now) == 1) {
            return new Claim(row.getId(), null);
        }
        throw new ApiException(HttpStatus.CONFLICT, "send_in_progress",
                "This message is still being sent. Check the conversation in a moment.");
    }

    private void complete(long id, ApiDtos.SendResponse response) {
        requests.findById(id).ifPresent(row ->
                row.complete(response.key(), response.threadId(), response.messageId(), LocalDateTime.now(clock)));
    }

    @Scheduled(cron = "${api.send-requests.prune-cron:0 15 3 * * ?}", zone = "UTC")
    @Transactional
    public void pruneExpired() {
        requests.deleteClaimedBefore(LocalDateTime.now(clock).minus(RETENTION));
    }
}
