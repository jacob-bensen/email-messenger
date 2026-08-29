package com.emailmessenger.api;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.User;
import com.emailmessenger.service.ConversationListItem;
import com.emailmessenger.web.ConversationListService;
import com.emailmessenger.web.ConversationListService.ChatFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.security.Principal;
import java.util.Locale;

/**
 * JSON chats endpoint for the mobile client. Wire contract in
 * {@code docs/API.md}. The endpoint delegates to the same
 * {@link ConversationListService} that backs the Thymeleaf {@code /chats}
 * page, so the list rules never diverge between web and mobile.
 */
@RestController
@RequestMapping("/api/v1")
class ApiChatsController {

    private static final int DEFAULT_SIZE = 30;
    private static final int MAX_SIZE = 100;

    private final ConversationListService conversationListService;
    private final UserService userService;

    ApiChatsController(ConversationListService conversationListService, UserService userService) {
        this.conversationListService = conversationListService;
        this.userService = userService;
    }

    @GetMapping("/chats")
    PagedResponse<ChatSummary> chats(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "" + DEFAULT_SIZE) int size,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "filter", required = false) String filter,
            @RequestParam(name = "mailbox", required = false) String mailbox,
            Principal principal) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be >= 0");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "size must be between 1 and " + MAX_SIZE);
        }
        ChatFilter chatFilter = parseFilter(filter);
        User owner = userService.requireByEmail(principal.getName());
        Page<ConversationListItem> src = conversationListService.list(
                owner, mailbox, q, chatFilter, PageRequest.of(page, size));
        return PagedResponse.of(src, ChatSummary::from);
    }

    private static ChatFilter parseFilter(String raw) {
        if (raw == null || raw.isBlank()) {
            return ChatFilter.ALL;
        }
        try {
            return ChatFilter.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "unknown filter: " + raw);
        }
    }
}
