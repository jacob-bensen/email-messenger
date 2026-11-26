package com.emailmessenger.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * Open SSE connections keyed by user id, shared by the website's
 * {@code /chats/events} and the API's {@code /api/v1/events}. A failed send
 * means a dead connection (closed tab, dropped network), so the emitter is
 * dropped; clients reconnect on their own.
 */
public final class SseSubscribers {

    private static final Logger log = LoggerFactory.getLogger(SseSubscribers.class);

    /** Generous cap; clients reconnect when the server closes the stream. */
    public static final long EMITTER_TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final Supplier<SseEmitter> emitterFactory;

    public SseSubscribers(Supplier<SseEmitter> emitterFactory) {
        this.emitterFactory = emitterFactory;
    }

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = emitterFactory.get();
        emitters.computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(e -> remove(userId, emitter));
        return emitter;
    }

    /** {@code event} is called once per emitter: a built event can't be sent twice. */
    public void send(Long userId, Supplier<SseEmitter.SseEventBuilder> event) {
        List<SseEmitter> forUser = emitters.get(userId);
        if (forUser == null) {
            return;
        }
        for (SseEmitter emitter : forUser) {
            try {
                emitter.send(event.get());
            } catch (Exception e) {
                remove(userId, emitter);
            }
        }
    }

    /**
     * Comment-only ping so idle connections aren't reaped by proxies, and so
     * dead emitters are discovered and dropped between events.
     */
    public void ping() {
        emitters.forEach((userId, forUser) -> send(userId, () -> SseEmitter.event().comment("ping")));
    }

    public boolean hasSubscribers(Long userId) {
        return activeCount(userId) > 0;
    }

    public int activeCount(Long userId) {
        List<SseEmitter> forUser = emitters.get(userId);
        return forUser == null ? 0 : forUser.size();
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
