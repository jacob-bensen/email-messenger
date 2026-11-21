package com.emailmessenger.api;

import com.emailmessenger.email.NewMailEvent;
import com.emailmessenger.repository.UserRepository;
import com.emailmessenger.web.ConversationListService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * docs/API.md §5.19: the app's live stream. Like the website's
 * {@code ChatEventStream}, but each {@code conversations} event carries the
 * changed list rows (built by the same {@link ConversationListService} as
 * §5.5) so the app updates rows in place instead of refetching the list.
 *
 * <p>Streams are tied to the bearer token that opened them: a token signed
 * out from another device (§5.17/5.18) or expired gets its streams closed at
 * the next event or keepalive.
 */
@Component
class ApiEventStream {

    private static final Logger log = LoggerFactory.getLogger(ApiEventStream.class);

    static final long EMITTER_TIMEOUT_MS = 30 * 60 * 1000L;

    record Subscriber(long tokenId, SseEmitter emitter) {}

    private final Map<Long, List<Subscriber>> subscribers = new ConcurrentHashMap<>();
    private final ConversationListService conversations;
    private final UserRepository users;
    private final ApiTokenService tokens;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate readTx;

    ApiEventStream(ConversationListService conversations, UserRepository users, ApiTokenService tokens,
                   ObjectMapper objectMapper, PlatformTransactionManager txManager) {
        this.conversations = conversations;
        this.users = users;
        this.tokens = tokens;
        this.objectMapper = objectMapper;
        // After-commit listeners still see the finished transaction bound to the
        // thread; a new one keeps these reads from joining it.
        this.readTx = new TransactionTemplate(txManager);
        this.readTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.readTx.setReadOnly(true);
    }

    SseEmitter subscribe(ApiPrincipal principal) {
        SseEmitter emitter = createEmitter();
        Subscriber subscriber = new Subscriber(principal.tokenId(), emitter);
        subscribers.computeIfAbsent(principal.userId(), id -> new CopyOnWriteArrayList<>()).add(subscriber);
        emitter.onCompletion(() -> remove(principal.userId(), subscriber));
        emitter.onTimeout(() -> remove(principal.userId(), subscriber));
        emitter.onError(e -> remove(principal.userId(), subscriber));
        try {
            // Tells the client the stream is live: it refetches once to catch
            // up on anything that happened while it was disconnected.
            emitter.send(SseEmitter.event().name("ready").data("{}"));
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
        String payload;
        try {
            payload = readTx.execute(status -> buildPayload(event));
        } catch (RuntimeException e) {
            log.warn("Could not build live update for user {}: {}", event.userId(), e.getMessage());
            return;
        }
        if (payload == null) {
            return;
        }
        for (Subscriber subscriber : forUser) {
            send(event.userId(), subscriber, SseEmitter.event().name("conversations").data(payload));
        }
    }

    private String buildPayload(NewMailEvent event) {
        return users.findById(event.userId())
                .map(owner -> new ApiDtos.ConversationsEvent(event.importedCount(),
                        conversations.rows(owner, event.conversationKeys()).stream()
                                .map(ApiDtos.ConversationRow::of).toList(),
                        ApiDtos.CountsResponse.of(conversations.counts(owner, null))))
                .map(this::toJson)
                .orElse(null);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    @Scheduled(fixedDelayString = "${mailbox.sse.keepalive-ms:25000}")
    void keepalive() {
        subscribers.forEach((userId, forUser) -> {
            for (Subscriber subscriber : forUser) {
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

    private void send(long userId, Subscriber subscriber, SseEmitter.SseEventBuilder event) {
        if (!tokens.isActive(subscriber.tokenId())) {
            remove(userId, subscriber);
            subscriber.emitter().complete();
            return;
        }
        try {
            subscriber.emitter().send(event);
        } catch (Exception e) {
            // Dead connection; the app reconnects on its own.
            remove(userId, subscriber);
        }
    }

    private void remove(long userId, Subscriber subscriber) {
        List<Subscriber> forUser = subscribers.get(userId);
        if (forUser == null) {
            return;
        }
        forUser.remove(subscriber);
        if (forUser.isEmpty()) {
            subscribers.remove(userId, forUser);
        }
    }
}
