package com.emailmessenger.api;

import com.emailmessenger.domain.User;
import com.emailmessenger.email.NewMailEvent;
import com.emailmessenger.repository.UserRepository;
import com.emailmessenger.web.ConversationListService;
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
 * docs/API.md §5.19: open {@code /api/v1/events} streams per user. Unlike the
 * website's count-only {@code new-mail} signal, each {@link NewMailEvent} is
 * turned into the changed conversation rows plus fresh counts, so a client
 * can update its list in place. Rows are built once per event, and only when
 * the user has an open stream.
 */
@Component
class ApiEventStream {

    private static final Logger log = LoggerFactory.getLogger(ApiEventStream.class);

    static final long EMITTER_TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final UserRepository users;
    private final ConversationListService conversations;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate readOnlyTx;

    ApiEventStream(UserRepository users, ConversationListService conversations,
                   ObjectMapper objectMapper, PlatformTransactionManager transactionManager) {
        this.users = users;
        this.conversations = conversations;
        this.objectMapper = objectMapper;
        this.readOnlyTx = new TransactionTemplate(transactionManager);
        // AFTER_COMMIT listeners still see the finished transaction's resources;
        // a fresh one keeps this read from joining it.
        this.readOnlyTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.readOnlyTx.setReadOnly(true);
    }

    SseEmitter subscribe(long userId) {
        SseEmitter emitter = createEmitter();
        List<SseEmitter> forUser = emitters.computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>());
        forUser.add(emitter);
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(e -> remove(userId, emitter));
        send(userId, emitter, SseEmitter.event().name("ready").data("{}"));
        return emitter;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    void onNewMail(NewMailEvent event) {
        List<SseEmitter> forUser = emitters.get(event.userId());
        if (forUser == null || forUser.isEmpty()) {
            return;
        }
        String payload = payloadFor(event);
        for (SseEmitter emitter : forUser) {
            send(event.userId(), emitter, SseEmitter.event().name("conversations-changed").data(payload));
        }
    }

    @Scheduled(fixedDelayString = "${mailbox.sse.keepalive-ms:25000}")
    void keepalive() {
        emitters.forEach((userId, forUser) -> {
            for (SseEmitter emitter : forUser) {
                send(userId, emitter, SseEmitter.event().comment("ping"));
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

    String payloadFor(NewMailEvent event) {
        ApiDtos.ConversationsChangedEvent body;
        try {
            body = readOnlyTx.execute(status -> {
                User owner = users.findById(event.userId()).orElse(null);
                if (owner == null) {
                    return null;
                }
                List<ApiDtos.ConversationRow> rows = conversations.items(owner, event.conversationKeys())
                        .stream().map(ApiDtos.ConversationRow::of).toList();
                return new ApiDtos.ConversationsChangedEvent(event.importedCount(), rows,
                        ApiDtos.CountsResponse.of(conversations.counts(owner, null)));
            });
        } catch (RuntimeException e) {
            log.warn("Could not build live rows for user {}: {}", event.userId(), e.getMessage());
            body = null;
        }
        if (body == null) {
            body = new ApiDtos.ConversationsChangedEvent(event.importedCount(), List.of(), null);
        }
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            return "{\"imported\":" + event.importedCount() + ",\"conversations\":[],\"counts\":null}";
        }
    }

    private void send(long userId, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (Exception e) {
            // Dead connection (app backgrounded, network dropped): the client
            // reconnects on its own and refetches on the next "ready".
            remove(userId, emitter);
        }
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
