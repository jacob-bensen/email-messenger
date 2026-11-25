package com.emailmessenger.api;

import com.emailmessenger.email.NewMailEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * docs/API.md §5.24: open {@code /api/v1/events} streams keyed by user id.
 * Each {@link NewMailEvent} becomes one {@code conversations-changed} event
 * carrying the changed list rows, built once per user and only when that
 * user has a stream open.
 */
@Component
class ApiEventStream {

    private static final Logger log = LoggerFactory.getLogger(ApiEventStream.class);

    static final String CONVERSATIONS_CHANGED = "conversations-changed";
    static final long EMITTER_TIMEOUT_MS = 30 * 60 * 1000L;

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
        emitter.onTimeout(() -> {
            remove(userId, emitter);
            // Completing here ends the stream cleanly instead of surfacing an
            // AsyncRequestTimeoutException to ApiExceptionHandler's catch-all.
            emitter.complete();
        });
        emitter.onError(e -> remove(userId, emitter));
        return emitter;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    void onNewMail(NewMailEvent event) {
        List<SseEmitter> forUser = emitters.get(event.userId());
        if (forUser == null || forUser.isEmpty() || event.conversationKeys().isEmpty()) {
            return;
        }
        String data;
        try {
            Optional<ApiDtos.ConversationsChanged> changes =
                    conversations.changes(event.userId(), event.conversationKeys());
            if (changes.isEmpty()) {
                return;
            }
            data = objectMapper.writeValueAsString(changes.get());
        } catch (JsonProcessingException | RuntimeException e) {
            // Never fail the poll that published the event.
            log.warn("Could not build live event for user {}: {}", event.userId(), e.getMessage());
            return;
        }
        for (SseEmitter emitter : forUser) {
            try {
                emitter.send(SseEmitter.event().name(CONVERSATIONS_CHANGED).data(data));
            } catch (Exception e) {
                remove(event.userId(), emitter);
            }
        }
    }

    /** Comment-only ping: keeps proxies from reaping idle streams and finds dead ones. */
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
        if (forUser != null) {
            forUser.remove(emitter);
            emitters.remove(userId, List.of());
        }
    }
}
