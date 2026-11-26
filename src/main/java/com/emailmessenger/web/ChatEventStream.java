package com.emailmessenger.web;

import com.emailmessenger.email.NewMailEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * The website's SSE connections (one per browser tab on the chats view).
 * {@link NewMailEvent}s fan out here as count-only {@code new-mail} events,
 * which the page turns into an in-place refresh — no reload.
 *
 * <p>The listener runs after commit when a transaction is active (so the
 * browser's follow-up fetch sees the imported rows) and immediately otherwise
 * (the scheduler's self-invoked poll path runs without one).
 */
@Component
class ChatEventStream {

    private final SseSubscribers subscribers = new SseSubscribers(this::createEmitter);

    SseEmitter subscribe(Long userId) {
        return subscribers.subscribe(userId);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    void onNewMail(NewMailEvent event) {
        subscribers.send(event.userId(), () -> SseEmitter.event()
                .name("new-mail")
                .data("{\"imported\":" + event.importedCount() + "}"));
    }

    @Scheduled(fixedDelayString = "${mailbox.sse.keepalive-ms:25000}")
    void keepalive() {
        subscribers.ping();
    }

    int activeCount(Long userId) {
        return subscribers.activeCount(userId);
    }

    /** Seam for tests to observe what gets sent. */
    protected SseEmitter createEmitter() {
        return new SseEmitter(SseSubscribers.EMITTER_TIMEOUT_MS);
    }
}
