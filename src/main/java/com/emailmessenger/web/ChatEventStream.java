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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry of open SSE connections, keyed by user id: browser tabs on the
 * chats view ({@link #subscribe}, {@code new-mail} events the page turns into
 * an in-place refresh) and API clients ({@link #subscribeApi}, docs/API.md
 * §5.24 {@code conversations-changed} events carrying the changed keys).
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

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    /** The API emitters, a subset of {@link #emitters}, which get the API event shape. */
    private final Set<SseEmitter> apiEmitters = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper;

    public ChatEventStream(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    SseEmitter subscribe(Long userId) {
        return register(userId, createEmitter());
    }

    /** docs/API.md §5.24. */
    public SseEmitter subscribeApi(Long userId) {
        SseEmitter emitter = createEmitter();
        apiEmitters.add(emitter);
        return register(userId, emitter);
    }

    private SseEmitter register(Long userId, SseEmitter emitter) {
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
        String webData = "{\"imported\":" + event.importedCount() + "}";
        String apiData = apiPayload(event);
        for (SseEmitter emitter : forUser) {
            try {
                // A builder is single-use (build() appends to it), so one per send.
                emitter.send(apiEmitters.contains(emitter)
                        ? SseEmitter.event().name("conversations-changed").data(apiData)
                        : SseEmitter.event().name("new-mail").data(webData));
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

    private String apiPayload(NewMailEvent event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("imported", event.importedCount());
        payload.put("keys", event.conversationKeys());
        try {
            return objectMapper.writeValueAsString(payload);
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
        apiEmitters.remove(emitter);
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
