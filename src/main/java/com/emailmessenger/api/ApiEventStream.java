package com.emailmessenger.api;

import com.emailmessenger.email.NewMailEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * docs/API.md §5.19: open {@code /api/v1/events} streams per user. Unlike the
 * website's count-only {@code new-mail} signal, each event carries the changed
 * list rows, built once per event and only when the user has a stream open.
 */
@Component
class ApiEventStream {

    private static final Logger log = LoggerFactory.getLogger(ApiEventStream.class);

    static final long EMITTER_TIMEOUT_MS = 30 * 60 * 1000L;
    static final String EVENT_NAME = "conversations-changed";

    private final ApiConversationService conversations;
    private final ObjectMapper objectMapper;
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    ApiEventStream(ApiConversationService conversations, ObjectMapper objectMapper) {
        this.conversations = conversations;
        this.objectMapper = objectMapper;
    }

    SseEmitter subscribe(long userId) {
        SseEmitter emitter = createEmitter();
        emitters.computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(e -> remove(userId, emitter));
        return emitter;
    }

    // After commit so the rows read here include the import.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    void onNewMail(NewMailEvent event) {
        List<SseEmitter> forUser = emitters.get(event.userId());
        if (forUser == null || forUser.isEmpty()) {
            return;
        }
        String data;
        try {
            data = objectMapper.writeValueAsString(
                    conversations.changes(event.userId(), event.importedCount(), event.conversationKeys()));
        } catch (Exception e) {
            log.warn("Could not build API event for user {}: {}", event.userId(), e.getMessage());
            return;
        }
        for (SseEmitter emitter : forUser) {
            try {
                emitter.send(SseEmitter.event().name(EVENT_NAME).data(data));
            } catch (Exception e) {
                remove(event.userId(), emitter);
            }
        }
    }

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

    int activeCount(long userId) {
        List<SseEmitter> forUser = emitters.get(userId);
        return forUser == null ? 0 : forUser.size();
    }

    /** Seam for tests to observe what gets sent. */
    protected SseEmitter createEmitter() {
        return new SseEmitter(EMITTER_TIMEOUT_MS);
    }

    private void remove(long userId, SseEmitter emitter) {
        List<SseEmitter> forUser = emitters.get(userId);
        if (forUser == null) {
            return;
        }
        forUser.remove(emitter);
        if (forUser.isEmpty()) {
            emitters.remove(userId, forUser);
        }
    }
}
