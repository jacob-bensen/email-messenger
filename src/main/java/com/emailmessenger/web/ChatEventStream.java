package com.emailmessenger.web;

import com.emailmessenger.email.NewMailEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry of open SSE connections (one per browser tab on the chats view),
 * keyed by user id. {@link NewMailEvent}s fan out here as {@code new-mail}
 * events, which the page turns into an in-place refresh — no reload.
 *
 * <p>The listener runs after commit when a transaction is active (so the
 * browser's follow-up fetch sees the imported rows) and immediately otherwise
 * (the scheduler's self-invoked poll path runs without one).
 */
@Component
class ChatEventStream {

    private static final Logger log = LoggerFactory.getLogger(ChatEventStream.class);

    /** Generous cap; the browser's EventSource reconnects transparently. */
    static final long EMITTER_TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    SseEmitter subscribe(Long userId) {
        SseEmitter emitter = createEmitter();
        List<SseEmitter> forUser =
                emitters.computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>());
        forUser.add(emitter);
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(e -> remove(userId, emitter));
        return emitter;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    void onNewMail(NewMailEvent event) {
        List<SseEmitter> forUser = emitters.get(event.userId());
        if (forUser == null) {
            return;
        }
        for (SseEmitter emitter : forUser) {
            try {
                emitter.send(SseEmitter.event()
                        .name("new-mail")
                        .data("{\"imported\":" + event.importedCount() + "}"));
            } catch (Exception e) {
                // Dead connection (closed tab, dropped network) — drop it; the
                // live tab's EventSource will have reconnected on its own.
                remove(event.userId(), emitter);
            }
        }
    }

    /**
     * Comment-only ping so idle connections aren't reaped by proxies, and so
     * dead emitters are discovered and dropped between mail events.
     */
    @Scheduled(fixedDelayString = "${mailbox.sse.keepalive-ms:25000}")
    void keepalive() {
        emitters.forEach((userId, forUser) -> {
            for (SseEmitter emitter : forUser) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (Exception e) {
                    remove(userId, emitter);
                }
            }
        });
    }

    int activeCount(Long userId) {
        List<SseEmitter> forUser = emitters.get(userId);
        return forUser == null ? 0 : forUser.size();
    }

    /** Seam for tests to observe what gets sent. */
    protected SseEmitter createEmitter() {
        return new SseEmitter(EMITTER_TIMEOUT_MS);
    }

    private void remove(Long userId, SseEmitter emitter) {
        List<SseEmitter> forUser = emitters.get(userId);
        if (forUser == null) {
            return;
        }
        forUser.remove(emitter);
        if (forUser.isEmpty() && emitters.remove(userId, forUser)) {
            log.debug("Last SSE subscriber gone for user {}", userId);
        }
    }
}
