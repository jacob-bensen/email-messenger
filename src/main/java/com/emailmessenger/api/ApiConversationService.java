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

import java.util.List;
import java.util.function.Supplier;

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

    private final UserRepository users;
    private final MailAccountService mailAccounts;
    private final MailboxPollingService polling;
    private final ConversationListService conversations;
    private final ChatService chats;
    private final ChatSendService sender;
    private final ApiSendIdempotency idempotency;

    ApiConversationService(UserRepository users, MailAccountService mailAccounts,
                           MailboxPollingService polling, ConversationListService conversations,
                           ChatService chats, ChatSendService sender, ApiSendIdempotency idempotency) {
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

    /** A send's response, and whether it is the stored answer to an earlier request (§5.8.1). */
    record Sent(ApiDtos.SendResponse response, boolean replayed) {}

    /**
     * Not transactional: {@link ChatSendService} keeps SMTP outside any database transaction, and an
     * idempotency claim must be committed before SMTP runs.
     */
    Sent send(ApiPrincipal principal, String key, Long mailboxId, String idempotencyKey,
              ApiDtos.SendMessageRequest request) {
        User owner = owner(principal);
        MailAccount selected = mailbox(owner, mailboxId);
        boolean asReply = request.asReply() != null && request.asReply();
        String fingerprint = fingerprint("messages", key, String.valueOf(mailboxId), request.body(),
                request.subject(), String.valueOf(asReply));
        return idempotent(owner, idempotencyKey, fingerprint, () -> sending(() -> sender.sendToConversation(
                owner, selected, key, request.body(), request.subject(), asReply, List.of())));
    }

    Sent start(ApiPrincipal principal, Long mailboxId, String idempotencyKey,
               ApiDtos.NewConversationRequest request) {
        User owner = owner(principal);
        MailAccount selected = mailbox(owner, mailboxId);
        String to = request.to() == null ? null : String.join("\u0001", request.to());
        String fingerprint = fingerprint("conversations", String.valueOf(mailboxId), to, request.subject(),
                request.body());
        return idempotent(owner, idempotencyKey, fingerprint, () -> sending(() -> sender.startConversation(
                owner, selected, request.to(), request.subject(), request.body(), List.of())));
    }

    private Sent idempotent(User owner, String idempotencyKey, String fingerprint,
                            Supplier<ApiDtos.SendResponse> send) {
        if (idempotencyKey == null) {
            return new Sent(send.get(), false);
        }
        ApiSendIdempotency.validate(idempotencyKey);
        ApiSendIdempotency.Claim claim;
        try {
            claim = idempotency.claim(owner.getId(), idempotencyKey, fingerprint);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "send_in_progress", "This message is still being sent.");
        }
        if (claim.replay() != null) {
            return new Sent(claim.replay(), true);
        }
        ApiDtos.SendResponse response;
        try {
            response = send.get();
        } catch (ApiException e) {
            // Every ApiException from sending() means the mail never left: free the key for a retry.
            idempotency.release(claim.id());
            throw e;
        }
        // Any other failure leaves the claim open on purpose: SMTP may have accepted the mail.
        idempotency.complete(claim.id(), response);
        return new Sent(response, false);
    }

    private static String fingerprint(String... parts) {
        StringBuilder canonical = new StringBuilder();
        for (String part : parts) {
            canonical.append(part == null ? "\u0002" : part).append('\u0000');
        }
        return ApiTokenService.hash(canonical.toString());
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
