package com.emailmessenger.api;

import com.emailmessenger.email.NewMailEvent;
import com.emailmessenger.web.SseSubscribers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * docs/API.md §5.24: the API's SSE connections (one per signed-in app). Each
 * {@link NewMailEvent} becomes a {@code new-mail} event carrying the changed
 * list rows, built only when the user has a stream open.
 */
@Component
class ApiEventStream {

    private static final Logger log = LoggerFactory.getLogger(ApiEventStream.class);

    private final SseSubscribers subscribers = new SseSubscribers(this::createEmitter);
    private final ApiConversationService conversations;

    ApiEventStream(ApiConversationService conversations) {
        this.conversations = conversations;
    }

    SseEmitter subscribe(Long userId) {
        return subscribers.subscribe(userId);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    void onNewMail(NewMailEvent event) {
        if (!subscribers.hasSubscribers(event.userId())) {
            return;
        }
        try {
            conversations.newMailUpdate(event.userId(), event.importedCount(), event.conversationKeys())
                    .ifPresent(update -> subscribers.send(event.userId(), () -> SseEmitter.event()
                            .name("new-mail")
                            .data(update, MediaType.APPLICATION_JSON)));
        } catch (RuntimeException e) {
            // After commit, an exception would surface in the poll that already succeeded.
            log.warn("Could not build the live update for user {}: {}", event.userId(), e.getMessage());
        }
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
