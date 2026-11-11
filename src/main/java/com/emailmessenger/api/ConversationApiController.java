package com.emailmessenger.api;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.MailboxPollingService;
import com.emailmessenger.web.ConversationListService;
import com.emailmessenger.web.ConversationListService.ChatFilter;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * The chats list for the mobile client: the same cross-account Dashboard feed
 * the web renders at {@code /chats}, read through the same
 * {@link ConversationListService}.
 */
@RestController
@RequestMapping("/api/v1/conversations")
class ConversationApiController {

    static final int DEFAULT_PAGE_SIZE = 30;
    static final int MAX_PAGE_SIZE = 50;

    private final ConversationListService conversationListService;
    private final MailboxPollingService mailboxPollingService;
    private final UserService users;

    ConversationApiController(ConversationListService conversationListService,
                              MailboxPollingService mailboxPollingService,
                              UserService users) {
        this.conversationListService = conversationListService;
        this.mailboxPollingService = mailboxPollingService;
        this.users = users;
    }

    @GetMapping
    ConversationPageResponse list(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size,
                                  @RequestParam(name = "q", required = false) String query,
                                  @RequestParam(name = "filter", required = false) String filter,
                                  Principal principal) {
        if (page < 0) {
            throw invalid("page must be 0 or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw invalid("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        ChatFilter chatFilter = parseFilter(filter);
        User owner = users.requireByEmail(principal.getName());
        // Same non-blocking refresh of due mailboxes the web list kicks off.
        mailboxPollingService.refreshDueForUserAsync(owner.getId());
        return ConversationPageResponse.of(
                conversationListService.list(owner, null, query, chatFilter, PageRequest.of(page, size)),
                conversationListService.counts(owner, null));
    }

    private static ChatFilter parseFilter(String raw) {
        if (raw == null || raw.isBlank()) {
            return ChatFilter.ALL;
        }
        for (ChatFilter f : ChatFilter.values()) {
            if (f.name().equalsIgnoreCase(raw.trim())) {
                return f;
            }
        }
        throw invalid("filter must be one of all, unread, awaiting, attachments");
    }

    private static ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "validation_failed", message);
    }
}
