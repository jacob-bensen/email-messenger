package com.emailmessenger.api;

import com.emailmessenger.email.NewMailEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * docs/API.md §5.21: open app streams per user. Unlike the website's
 * {@code ChatEventStream} (a count only), each {@link NewMailEvent} becomes
 * the changed conversation rows, so a client can patch its list in place.
 * The keepalive also closes streams whose token was revoked or expired, so a
 * signed-out device stops receiving mail within one keepalive interval.
 */
@Component
class ApiEventStream {

    private static final Logger log = LoggerFactory.getLogger(ApiEventStream.class);

    static final long EMITTER_TIMEOUT_MS = 30 * 60 * 1000L;

    private record Subscriber(long tokenId, SseEmitter emitter) {}

    private final Map<Long, List<Subscriber>> subscribers = new ConcurrentHashMap<>();
    private final ApiEventPayloads payloads;
    private final ApiTokenService tokens;
    private final TransactionTemplate freshReadOnly;

    ApiEventStream(ApiEventPayloads payloads, ApiTokenService tokens, PlatformTransactionManager transactions) {
        this.payloads = payloads;
        this.tokens = tokens;
        // The after-commit listener runs while the poll's finished transaction
        // is still bound; joining it is unsafe, so the reads get their own.
        this.freshReadOnly = new TransactionTemplate(transactions);
        this.freshReadOnly.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.freshReadOnly.setReadOnly(true);
    }

    SseEmitter subscribe(ApiPrincipal principal) {
        SseEmitter emitter = createEmitter();
        Subscriber subscriber = new Subscriber(principal.tokenId(), emitter);
        subscribers.computeIfAbsent(principal.userId(), id -> new CopyOnWriteArrayList<>()).add(subscriber);
        emitter.onCompletion(() -> remove(principal.userId(), subscriber));
        emitter.onTimeout(() -> remove(principal.userId(), subscriber));
        emitter.onError(e -> remove(principal.userId(), subscriber));
        try {
            emitter.send(SseEmitter.event().name("ready").data(Map.of(), MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            remove(principal.userId(), subscriber);
        }
        return emitter;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    void onNewMail(NewMailEvent event) {
        List<Subscriber> forUser = subscribers.get(event.userId());
        if (forUser == null || forUser.isEmpty()) {
            return;
        }
        Optional<ApiDtos.ConversationsEvent> data;
        try {
            data = freshReadOnly.execute(status -> payloads.conversationsChanged(
                    event.userId(), event.importedCount(), event.conversationKeys()));
        } catch (Exception e) {
            // Never fail the poll that published this; clients refetch on reconnect.
            log.warn("Could not build the live event for user {}: {}", event.userId(), e.getMessage());
            return;
        }
        data.ifPresent(payload -> {
            for (Subscriber subscriber : forUser) {
                send(event.userId(), subscriber,
                        SseEmitter.event().name("conversations").data(payload, MediaType.APPLICATION_JSON));
            }
        });
    }

    @Scheduled(fixedDelayString = "${mailbox.sse.keepalive-ms:25000}")
    void keepalive() {
        subscribers.forEach((userId, forUser) -> {
            for (Subscriber subscriber : forUser) {
                if (!tokens.isActive(subscriber.tokenId())) {
                    remove(userId, subscriber);
                    subscriber.emitter().complete();
                    continue;
                }
                send(userId, subscriber, SseEmitter.event().comment("ping"));
            }
        });
    }

    int activeCount(long userId) {
        List<Subscriber> forUser = subscribers.get(userId);
        return forUser == null ? 0 : forUser.size();
    }

    /** Seam for tests to observe what gets sent. */
    protected SseEmitter createEmitter() {
        return new SseEmitter(EMITTER_TIMEOUT_MS);
    }

    private void send(Long userId, Subscriber subscriber, SseEmitter.SseEventBuilder event) {
        try {
            subscriber.emitter().send(event);
        } catch (Exception e) {
            remove(userId, subscriber);
        }
    }

    private void remove(Long userId, Subscriber subscriber) {
        List<Subscriber> forUser = subscribers.get(userId);
        if (forUser != null) {
            forUser.remove(subscriber);
            if (forUser.isEmpty()) {
                subscribers.remove(userId, forUser);
            }
        }
    }
}
