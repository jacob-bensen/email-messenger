package com.emailmessenger.api;

import com.emailmessenger.domain.ApiSendRequest;
import com.emailmessenger.repository.ApiSendRequestRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * The {@code Idempotency-Key} rule of docs/API.md §5.8a. The key is claimed in
 * its own short transaction before SMTP and completed in another after the
 * message is recorded, so no transaction spans the mail server.
 */
@Component
class ApiSendIdempotency {

    static final Duration RETENTION = Duration.ofHours(24);
    private static final Pattern KEY = Pattern.compile("[A-Za-z0-9_-]{16,64}");

    private final ApiSendRequestRepository requests;
    private final TransactionTemplate transactions;
    private final Clock clock;

    ApiSendIdempotency(ApiSendRequestRepository requests, PlatformTransactionManager transactionManager,
                       Clock clock) {
        this.requests = requests;
        this.transactions = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    /**
     * Runs {@code send} at most once per (user, key). {@code fingerprint} is
     * every request input that changes what gets sent; a key reused with a
     * different one is rejected rather than silently answered with the old result.
     */
    ApiDtos.SendResponse once(long userId, String key, String fingerprint, Supplier<ApiDtos.SendResponse> send) {
        if (key == null) {
            return send.get();
        }
        if (!KEY.matcher(key).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "validation_failed",
                    "Idempotency-Key must be 16–64 letters, digits, '-' or '_'.");
        }
        String hash = sha256(fingerprint);
        Claim claim = claim(userId, key, hash);
        if (claim.existing() != null) {
            return replay(claim.existing(), hash);
        }
        ApiDtos.SendResponse response;
        try {
            response = send.get();
        } catch (ApiException e) {
            // Every ApiException from the send path means nothing went out, so
            // the same key may be tried again. Anything else (e.g. recording
            // failed after SMTP accepted) keeps the claim: a retry must not resend.
            transactions.executeWithoutResult(s -> requests.deleteById(claim.id()));
            throw e;
        }
        transactions.executeWithoutResult(s -> requests.findById(claim.id()).ifPresent(r ->
                r.complete(response.key(), response.threadId(), response.messageId(), LocalDateTime.now(clock))));
        return response;
    }

    private Claim claim(long userId, String key, String hash) {
        try {
            return transactions.execute(s -> {
                LocalDateTime now = LocalDateTime.now(clock);
                requests.deleteByUserIdCreatedBefore(userId, now.minus(RETENTION));
                return requests.findByUserIdAndIdempotencyKey(userId, key)
                        .map(Claim::of)
                        .orElseGet(() -> new Claim(
                                requests.saveAndFlush(new ApiSendRequest(userId, key, hash, now)).getId(), null));
            });
        } catch (DataIntegrityViolationException e) {
            // A concurrent request with the same key won the insert.
            throw inProgress();
        }
    }

    private static ApiDtos.SendResponse replay(ApiSendRequest existing, String hash) {
        if (!existing.getRequestHash().equals(hash)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "idempotency_key_reused",
                    "This Idempotency-Key was already used for a different message.");
        }
        if (existing.getStatus() != ApiSendRequest.Status.COMPLETED) {
            throw inProgress();
        }
        return new ApiDtos.SendResponse(existing.getConversationKey(), existing.getThreadId(),
                existing.getMessageId());
    }

    private static ApiException inProgress() {
        return new ApiException(HttpStatus.CONFLICT, "send_in_progress",
                "This message is still being sent. Check the conversation in a moment.");
    }

    /** Length-prefixes each part so two different requests never hash the same text. */
    static String fingerprint(Object... parts) {
        StringBuilder out = new StringBuilder();
        for (Object part : parts) {
            String text = Objects.toString(part, "\u0000null");
            out.append(text.length()).append(':').append(text).append('\n');
        }
        return out.toString();
    }

    private static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private record Claim(Long id, ApiSendRequest existing) {
        static Claim of(ApiSendRequest existing) {
            return new Claim(existing.getId(), existing);
        }
    }
}
