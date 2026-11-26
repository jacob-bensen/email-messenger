package com.emailmessenger.web;

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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry of open SSE connections (browser tabs on the chats view and API
 * clients on {@code /api/v1/events}), keyed by user id. {@link NewMailEvent}s
 * and {@link ConversationChangedEvent}s fan out here as {@code new-mail} and
 * {@code conversation-changed} events ({@code docs/API.md} §5.24).
 *
 * <p>The listener runs after commit when a transaction is active (so the
 * browser's follow-up fetch sees the imported rows) and immediately otherwise
 * (the scheduler's self-invoked poll path runs without one).
 */
@Component
public class ChatEventStream {

    private static final Logger log = LoggerFactory.getLogger(ChatEventStream.class);

    /** Generous cap; the browser's EventSource reconnects transparently. */
    static final long EMITTER_TIMEOUT_MS = 30 * 60 * 1000L;

    private static final ObjectMapper JSON = new ObjectMapper();

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId) {
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
    public void onNewMail(NewMailEvent event) {
        broadcast(event.userId(), "new-mail", newMailData(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    void onConversationChanged(ConversationChangedEvent event) {
        broadcast(event.userId(), "conversation-changed", conversationChangedData(event));
    }

    private void broadcast(Long userId, String name, String data) {
        List<SseEmitter> forUser = emitters.get(userId);
        if (forUser == null) {
            return;
        }
        for (SseEmitter emitter : forUser) {
            try {
                emitter.send(SseEmitter.event().name(name).data(data));
            } catch (Exception e) {
                // Dead connection (closed tab, dropped network) — drop it; the
                // live tab's EventSource will have reconnected on its own.
                remove(userId, emitter);
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

    static String newMailData(NewMailEvent event) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("imported", event.importedCount());
        body.put("conversationKeys", event.conversationKeys());
        return json(body);
    }

    static String conversationChangedData(ConversationChangedEvent event) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("change", event.change().wire());
        body.put("conversationKeys", event.conversationKeys());
        return json(body);
    }

    private static String json(Map<String, Object> body) {
        try {
            return JSON.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
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
