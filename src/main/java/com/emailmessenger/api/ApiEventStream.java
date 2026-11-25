package com.emailmessenger.api;

import com.emailmessenger.email.NewMailEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
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
 * A {@link NewMailEvent} becomes one {@code conversations-changed} event
 * carrying the changed list rows, built once per user and sent to each of
 * their devices. The website's {@code /chats/events} keeps its own count-only
 * stream ({@code web.ChatEventStream}).
 */
@Component
class ApiEventStream {

    private static final Logger log = LoggerFactory.getLogger(ApiEventStream.class);

    static final String EVENT_NAME = "conversations-changed";
    static final long EMITTER_TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final ApiConversationService conversations;
    private final ObjectMapper objectMapper;

    ApiEventStream(ApiConversationService conversations, ObjectMapper objectMapper) {
        this.conversations = conversations;
        this.objectMapper = objectMapper;
    }

    SseEmitter subscribe(Long userId) {
        SseEmitter emitter = createEmitter();
        emitters.computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(e -> remove(userId, emitter));
        return emitter;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    void onNewMail(NewMailEvent event) {
        List<SseEmitter> forUser = emitters.get(event.userId());
        if (forUser == null || forUser.isEmpty()) {
            return;
        }
        String data;
        try {
            Optional<ApiDtos.ConversationsChanged> changed =
                    conversations.changed(event.userId(), event.importedCount(), event.conversationKeys());
            if (changed.isEmpty()) {
                return;
            }
            data = objectMapper.writeValueAsString(changed.get());
        } catch (Exception e) {
            // Never fail the poll that published the event; clients catch up on their next refetch.
            log.warn("Could not build live event for user {}: {}", event.userId(), e.getMessage());
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

    /**
     * Ends every stream when the app shuts down. ContextClosedEvent comes
     * before the web server's graceful shutdown, which would otherwise wait
     * on these never-ending responses; clients reconnect to the next instance.
     */
    @EventListener(ContextClosedEvent.class)
    void completeAll() {
        emitters.values().forEach(forUser -> forUser.forEach(SseEmitter::complete));
        emitters.clear();
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
        if (forUser.isEmpty()) {
            emitters.remove(userId, forUser);
        }
    }
}
