package com.emailmessenger.api;

import com.emailmessenger.domain.ApiSendRequest;
import com.emailmessenger.repository.ApiSendRequestRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * {@code Idempotency-Key} for API sends (docs/API.md §5.8.1). Claim, release and
 * complete each run in their own short transaction so the SMTP call in between
 * holds none, as {@code ChatSendService} requires.
 */
@Service
class ApiSendIdempotency {

    static final Duration KEY_LIFETIME = Duration.ofHours(24);
    private static final Pattern VALID_KEY = Pattern.compile("[A-Za-z0-9._:-]{1,100}");

    private final ApiSendRequestRepository requests;
    private final Clock clock;
    private final TransactionTemplate tx;

    ApiSendIdempotency(ApiSendRequestRepository requests, Clock clock, PlatformTransactionManager txManager) {
        this.requests = requests;
        this.clock = clock;
        this.tx = new TransactionTemplate(txManager);
    }

    /** What the controller answers: the body, and whether it was replayed from an earlier request. */
    record Outcome(ApiDtos.SendResponse response, boolean replayed) {}

    /**
     * Runs {@code send} at most once per (user, key, fingerprint). A {@code null}
     * key just sends. The key is released when the send fails before or at the
     * mail server, so the client may retry; any other failure leaves it claimed
     * because the mail may already have gone out.
     */
    Outcome run(long userId, String key, List<?> fingerprint, Supplier<ApiDtos.SendResponse> send) {
        if (key == null) {
            return new Outcome(send.get(), false);
        }
        if (!VALID_KEY.matcher(key).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "validation_failed",
                    "Idempotency-Key must be 1–100 characters from A–Z, a–z, 0–9, '-', '_', '.', ':'.");
        }
        String hash = fingerprint(fingerprint);
        Claim claim;
        try {
            claim = tx.execute(status -> claim(userId, key, hash));
        } catch (DataIntegrityViolationException raced) {
            throw inProgress();
        }
        switch (claim.state()) {
            case REPLAY -> {
                return new Outcome(claim.replay(), true);
            }
            case IN_PROGRESS -> throw inProgress();
            case KEY_REUSED -> throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "idempotency_key_reused",
                    "This Idempotency-Key was already used for a different message.");
            case CLAIMED -> { }
        }
        ApiDtos.SendResponse response;
        try {
            response = send.get();
        } catch (ApiException | MailException e) {
            // Rejected before sending, or refused by the mail server: nothing went out.
            tx.executeWithoutResult(status -> requests.deleteById(claim.id()));
            throw e;
        }
        tx.executeWithoutResult(status -> requests.findById(claim.id()).ifPresent(row -> row.complete(
                response.key(), response.threadId(), response.messageId(), LocalDateTime.now(clock))));
        return new Outcome(response, false);
    }

    private enum State { CLAIMED, REPLAY, IN_PROGRESS, KEY_REUSED }

    private record Claim(State state, Long id, ApiDtos.SendResponse replay) {}

    private Claim claim(long userId, String key, String hash) {
        LocalDateTime now = LocalDateTime.now(clock);
        requests.deleteByUserIdCreatedBefore(userId, now.minus(KEY_LIFETIME));
        var existing = requests.findByUserIdAndIdempotencyKey(userId, key);
        if (existing.isEmpty()) {
            return new Claim(State.CLAIMED,
                    requests.saveAndFlush(new ApiSendRequest(userId, key, hash, now)).getId(), null);
        }
        ApiSendRequest row = existing.get();
        if (!row.getRequestHash().equals(hash)) {
            return new Claim(State.KEY_REUSED, null, null);
        }
        if (!row.isCompleted()) {
            return new Claim(State.IN_PROGRESS, null, null);
        }
        return new Claim(State.REPLAY, null,
                new ApiDtos.SendResponse(row.getConversationKey(), row.getThreadId(), row.getMessageId()));
    }

    private static ApiException inProgress() {
        return new ApiException(HttpStatus.CONFLICT, "send_in_progress",
                "This message is still being sent, or we couldn't tell whether it went out. "
                        + "Refresh the conversation before sending it again.");
    }

    /** SHA-256 over the fields, each length-prefixed so field boundaries can't be shifted. */
    static String fingerprint(List<?> fields) {
        StringBuilder canonical = new StringBuilder();
        for (Object field : fields) {
            String value = Objects.toString(field, "\u0000null");
            canonical.append(value.length()).append(':').append(value).append('|');
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
