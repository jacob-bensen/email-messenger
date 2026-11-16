package com.emailmessenger.api;

import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.MailAccountService;
import com.emailmessenger.email.MailboxPollingService;
import com.emailmessenger.repository.UserRepository;
import com.emailmessenger.service.ChatConversation;
import com.emailmessenger.service.ConversationListItem;
import com.emailmessenger.web.ChatService;
import com.emailmessenger.web.ConversationListService;
import com.emailmessenger.web.ConversationListService.ChatFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * docs/API.md §5.5–§5.7. Delegates to the same {@link ConversationListService},
 * {@link ChatService} and scope rule as the website's {@code /chats}; this
 * class only validates input and maps to the wire shape.
 */
@Service
class ApiConversationService {

    static final int DEFAULT_PAGE_SIZE = 30;
    static final int MAX_PAGE_SIZE = 100;

    private final UserRepository users;
    private final MailAccountService mailAccounts;
    private final MailboxPollingService polling;
    private final ConversationListService conversations;
    private final ChatService chats;

    ApiConversationService(UserRepository users, MailAccountService mailAccounts,
                           MailboxPollingService polling, ConversationListService conversations,
                           ChatService chats) {
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
        ChatConversation chat = chats.buildFor(owner, scope(owner, mailboxId), key);
        if (chat == null) {
            throw conversationNotFound();
        }
        return ApiDtos.ConversationDetail.of(chat);
    }

    @Transactional
    void markRead(ApiPrincipal principal, String key, Long mailboxId) {
        User owner = owner(principal);
        if (!chats.markRead(owner, scope(owner, mailboxId), key)) {
            throw conversationNotFound();
        }
    }

    private User owner(ApiPrincipal principal) {
        return users.findById(principal.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "unauthorized", "Sign in again."));
    }

    private String scope(User owner, Long mailboxId) {
        if (mailboxId == null) {
            return null;
        }
        List<MailAccount> accounts = mailAccounts.list(owner);
        MailAccount selected = accounts.stream()
                .filter(a -> a.getId().equals(mailboxId))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "not_found", "No such mailbox."));
        return ConversationListService.scopeAddress(selected, accounts);
    }

    private static ApiException conversationNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "not_found", "No such conversation.");
    }

    private static ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "validation_failed", message);
    }
}
