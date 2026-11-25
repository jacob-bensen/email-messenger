package com.emailmessenger.api;

import com.emailmessenger.email.NewMailEvent;
import com.emailmessenger.repository.ApiTokenRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Open {@code GET /api/v1/events} streams (docs/API.md §5.24), keyed by user.
 * Each stream remembers the bearer token that opened it so a device that is
 * signed out stops receiving events at the next keepalive, not only when the
 * stream times out.
 */
@Component
class ApiEventStream {

    static final long EMITTER_TIMEOUT_MS = 30 * 60 * 1000L;
    static final String EVENT_NAME = "conversations-changed";

    record Subscription(long tokenId, SseEmitter emitter) {}

    private final Map<Long, List<Subscription>> subscriptions = new ConcurrentHashMap<>();
    private final ApiTokenRepository tokens;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    ApiEventStream(ApiTokenRepository tokens, ObjectMapper objectMapper, Clock clock) {
        this.tokens = tokens;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    SseEmitter subscribe(ApiPrincipal principal) {
        SseEmitter emitter = createEmitter();
        Subscription subscription = new Subscription(principal.tokenId(), emitter);
        subscriptions.computeIfAbsent(principal.userId(), id -> new CopyOnWriteArrayList<>()).add(subscription);
        emitter.onCompletion(() -> remove(principal.userId(), subscription));
        emitter.onTimeout(() -> remove(principal.userId(), subscription));
        emitter.onError(e -> remove(principal.userId(), subscription));
        return emitter;
    }

    /** After commit, so the client's follow-up fetch sees the imported rows. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    void onNewMail(NewMailEvent event) {
        List<Subscription> forUser = subscriptions.get(event.userId());
        if (forUser == null) {
            return;
        }
        String data = payload(event);
        for (Subscription subscription : forUser) {
            send(event.userId(), subscription, SseEmitter.event().name(EVENT_NAME).data(data));
        }
    }

    /**
     * Comment ping so proxies keep idle streams open; also the point where a
     * stream whose token was revoked (sign-out, device removed, password
     * change) or has expired is closed.
     */
    @Scheduled(fixedDelayString = "${mailbox.sse.keepalive-ms:25000}")
    void keepalive() {
        Set<Long> open = subscriptions.values().stream()
                .flatMap(List::stream)
                .map(Subscription::tokenId)
                .collect(Collectors.toSet());
        if (open.isEmpty()) {
            return;
        }
        Set<Long> active = Set.copyOf(tokens.findActiveIdsIn(open, LocalDateTime.now(clock)));
        subscriptions.forEach((userId, forUser) -> {
            for (Subscription subscription : forUser) {
                if (active.contains(subscription.tokenId())) {
                    send(userId, subscription, SseEmitter.event().comment("ping"));
                } else {
                    remove(userId, subscription);
                    subscription.emitter().complete();
                }
            }
        });
    }

    int activeCount(long userId) {
        List<Subscription> forUser = subscriptions.get(userId);
        return forUser == null ? 0 : forUser.size();
    }

    /** Seam for tests to observe what gets sent. */
    protected SseEmitter createEmitter() {
        return new SseEmitter(EMITTER_TIMEOUT_MS);
    }

    private String payload(NewMailEvent event) {
        try {
            return objectMapper.writeValueAsString(new ApiDtos.ConversationsChangedEvent(
                    event.importedCount(), event.conversationKeys().stream().sorted().toList()));
        } catch (Exception e) {
            throw new IllegalStateException("Could not serialize event", e);
        }
    }

    private void send(long userId, Subscription subscription, SseEmitter.SseEventBuilder event) {
        try {
            subscription.emitter().send(event);
        } catch (Exception e) {
            // Dead connection (app backgrounded, network dropped); the client reconnects.
            remove(userId, subscription);
        }
    }

    private void remove(long userId, Subscription subscription) {
        List<Subscription> forUser = subscriptions.get(userId);
        if (forUser == null) {
            return;
        }
        forUser.remove(subscription);
        if (forUser.isEmpty()) {
            subscriptions.remove(userId, forUser);
        }
    }
}
