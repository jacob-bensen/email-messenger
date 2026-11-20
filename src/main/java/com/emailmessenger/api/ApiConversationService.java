package com.emailmessenger.api;

import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.MailAccountService;
import com.emailmessenger.email.MailboxPollingService;
import com.emailmessenger.repository.UserRepository;
import com.emailmessenger.service.ChatConversation;
import com.emailmessenger.service.ConversationListItem;
import com.emailmessenger.web.ChatSendService;
import com.emailmessenger.web.ChatService;
import com.emailmessenger.web.ConversationListService;
import com.emailmessenger.web.ConversationListService.ChatFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * docs/API.md §5.5–5.9. Delegates to the same {@link ConversationListService},
 * {@link ChatService}, {@link ChatSendService} and scope rule as the website's {@code /chats}; this
 * class only validates input and maps to the wire shape.
 */
@Service
class ApiConversationService {

    private static final Logger log = LoggerFactory.getLogger(ApiConversationService.class);

    static final int DEFAULT_PAGE_SIZE = 30;
    static final int MAX_PAGE_SIZE = 100;
    private static final Pattern IDEMPOTENCY_KEY = Pattern.compile("[A-Za-z0-9_.:-]{8,100}");

    private final UserRepository users;
    private final MailAccountService mailAccounts;
    private final MailboxPollingService polling;
    private final ConversationListService conversations;
    private final ChatService chats;
    private final ChatSendService sender;
    private final SendIdempotencyStore idempotency;

    ApiConversationService(UserRepository users, MailAccountService mailAccounts,
                           MailboxPollingService polling, ConversationListService conversations,
                           ChatService chats, ChatSendService sender, SendIdempotencyStore idempotency) {
        this.sender = sender;
        this.idempotency = idempotency;
        this.users = users;
        this.mailAccounts = mailAccounts;
        this.polling = polling;
        this.conversations = conversations;
        this.chats = chats;
    }

    @Transactional(readOnly = true)
    ApiDtos.ConversationPage list(ApiPrincipal principal, int page, int size, String query,
                                  Long mailboxId, String filterParam) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw invalid("page must be ≥ 0 and size between 1 and " + MAX_PAGE_SIZE + ".");
        }
        ChatFilter filter = filterParam == null || filterParam.isBlank()
                ? ChatFilter.ALL
                : ChatFilter.parse(filterParam).orElseThrow(() -> invalid("Unknown filter."));
        User owner = owner(principal);
        String scope = scope(owner, mailboxId);
        polling.refreshDueForUserAsync(owner.getId());
        Page<ConversationListItem> rows = conversations.list(owner, scope, query, filter, PageRequest.of(page, size));
        return ApiDtos.ConversationPage.of(rows, conversations.counts(owner, scope));
    }

    @Transactional(readOnly = true)
    ApiDtos.ConversationDetail get(ApiPrincipal principal, String key, Long mailboxId) {
        User owner = owner(principal);
        String scope = scope(owner, mailboxId);
        ChatConversation chat = scope != null ? chats.buildFor(owner, scope, key) : chats.buildFor(owner, key);
        if (chat == null) {
            throw noSuchConversation();
        }
        return ApiDtos.ConversationDetail.of(chat);
    }

    @Transactional
    void markRead(ApiPrincipal principal, String key, Long mailboxId) {
        User owner = owner(principal);
        if (chats.markRead(owner, scope(owner, mailboxId), key) == 0) {
            throw noSuchConversation();
        }
    }

    /** Not transactional: {@link ChatSendService} keeps SMTP outside any database transaction. */
    ApiDtos.SendResponse send(ApiPrincipal principal, String key, Long mailboxId,
                              ApiDtos.SendMessageRequest request, String idempotencyKey) {
        User owner = owner(principal);
        MailAccount selected = mailbox(owner, mailboxId);
        boolean asReply = request.asReply() != null && request.asReply();
        String fingerprint = fingerprint("reply", key, mailboxId, request.body(), request.subject(),
                String.valueOf(asReply));
        return idempotently(owner, idempotencyKey, fingerprint, () -> sending(() -> sender.sendToConversation(
                owner, selected, key, request.body(), request.subject(), asReply, List.of())));
    }

    ApiDtos.SendResponse start(ApiPrincipal principal, Long mailboxId, ApiDtos.NewConversationRequest request,
                               String idempotencyKey) {
        User owner = owner(principal);
        MailAccount selected = mailbox(owner, mailboxId);
        List<String> parts = new ArrayList<>(List.of("new", String.valueOf(mailboxId),
                String.valueOf(request.subject()), String.valueOf(request.body())));
        if (request.to() != null) {
            parts.addAll(request.to());
        }
        String fingerprint = fingerprint(parts.toArray(String[]::new));
        return idempotently(owner, idempotencyKey, fingerprint, () -> sending(() -> sender.startConversation(
                owner, selected, request.to(), request.subject(), request.body(), List.of())));
    }

    /**
     * docs/API.md §5.8.1. A rejected send ({@link ApiException}: nothing went out) releases the key;
     * any other failure keeps it unfinished, because the mail may already have been accepted.
     */
    private ApiDtos.SendResponse idempotently(User owner, String idempotencyKey, String fingerprint,
                                              Supplier<ApiDtos.SendResponse> send) {
        if (idempotencyKey == null) {
            return send.get();
        }
        if (!IDEMPOTENCY_KEY.matcher(idempotencyKey).matches()) {
            throw invalid("Idempotency-Key must be 8–100 characters: letters, digits, _ - . :");
        }
        long userId = owner.getId();
        SendIdempotencyStore.Prior prior;
        try {
            prior = idempotency.reserve(userId, idempotencyKey, fingerprint);
        } catch (DataIntegrityViolationException e) {
            throw sendInProgress();
        }
        if (prior != null) {
            if (!prior.requestHash().equals(fingerprint)) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "idempotency_key_reused",
                        "This Idempotency-Key was already used for a different message.");
            }
            if (prior.response() == null) {
                throw sendInProgress();
            }
            return prior.response();
        }
        ApiDtos.SendResponse response;
        try {
            response = send.get();
        } catch (ApiException e) {
            idempotency.release(userId, idempotencyKey);
            throw e;
        }
        idempotency.complete(userId, idempotencyKey, response);
        return response;
    }

    private static ApiException sendInProgress() {
        return new ApiException(HttpStatus.CONFLICT, "send_in_progress",
                "This message is still being sent, or may already have been sent. Check the conversation.");
    }

    /** SHA-256 over length-prefixed parts, so no choice of field values can collide by concatenation. */
    private static String fingerprint(Object... parts) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (Object part : parts) {
                byte[] bytes = String.valueOf(part).getBytes(StandardCharsets.UTF_8);
                digest.update((bytes.length + ":").getBytes(StandardCharsets.US_ASCII));
                digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ApiDtos.SendResponse sending(Supplier<ChatSendService.SendResult> send) {
        try {
            return ApiDtos.SendResponse.of(send.get());
        } catch (ChatSendService.SendRejectedException e) {
            throw switch (e.reason()) {
                case NO_SUCH_CONVERSATION -> noSuchConversation();
                case NO_MEMBERS -> new ApiException(HttpStatus.CONFLICT, "no_recipients", e.getMessage());
                // The API has no attachments, so "a message or an attachment" would mislead.
                case EMPTY_MESSAGE -> invalid("Write a message.");
                case NO_RECIPIENTS, BODY_TOO_LONG -> invalid(e.getMessage());
            };
        } catch (MailException e) {
            log.warn("API send failed: {}", e.getMessage());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "send_failed",
                    "Your mail server didn't accept the message. Nothing was sent.");
        }
    }

    private User owner(ApiPrincipal principal) {
        return users.findById(principal.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "unauthorized", "Sign in again."));
    }

    /** The mailbox address to scope by (§5.5 scope rule), or {@code null} for all mailboxes. */
    private String scope(User owner, Long mailboxId) {
        return ConversationListService.scopeAddress(mailbox(owner, mailboxId), mailAccounts.list(owner));
    }

    /** The user's mailbox {@code mailboxId}, or {@code null} when none was asked for. */
    private MailAccount mailbox(User owner, Long mailboxId) {
        if (mailboxId == null) {
            return null;
        }
        return mailAccounts.list(owner).stream()
                .filter(a -> a.getId().equals(mailboxId))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "not_found", "No such mailbox."));
    }

    private static ApiException noSuchConversation() {
        return new ApiException(HttpStatus.NOT_FOUND, "not_found", "No such conversation.");
    }

    private static ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "validation_failed", message);
    }
}
