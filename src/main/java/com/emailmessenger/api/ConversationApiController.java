package com.emailmessenger.api;

import com.emailmessenger.api.ConversationDtos.ConversationDetail;
import com.emailmessenger.api.ConversationDtos.ConversationPage;
import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.User;
import com.emailmessenger.service.ChatConversation;
import com.emailmessenger.web.ChatService;
import com.emailmessenger.web.ConversationListService;
import com.emailmessenger.web.ConversationListService.ChatFilter;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Locale;
import java.util.NoSuchElementException;

/**
 * Read side of the chats experience for the mobile client. Backed by the same
 * {@link ConversationListService} / {@link ChatService} the /chats pages use;
 * always the cross-account (Hub) view — per-mailbox scoping is not exposed yet.
 */
@RestController
@RequestMapping("/api/v1/conversations")
class ConversationApiController {

    static final int DEFAULT_PAGE_SIZE = 30;
    static final int MAX_PAGE_SIZE = 100;

    private final ConversationListService conversationListService;
    private final ChatService chatService;
    private final UserService userService;

    ConversationApiController(ConversationListService conversationListService,
                              ChatService chatService,
                              UserService userService) {
        this.conversationListService = conversationListService;
        this.chatService = chatService;
        this.userService = userService;
    }

    @GetMapping
    ConversationPage list(@RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size,
                          @RequestParam(name = "q", required = false) String query,
                          @RequestParam(name = "filter", defaultValue = "all") String filter,
                          Principal principal) {
        User owner = userService.requireByEmail(principal.getName());
        PageRequest pageable = PageRequest.of(Math.max(0, page),
                Math.min(Math.max(1, size), MAX_PAGE_SIZE));
        return ConversationPage.from(
                conversationListService.list(owner, null, query, parseFilter(filter), pageable),
                conversationListService.counts(owner, null));
    }

    @GetMapping("/{key}")
    ConversationDetail detail(@PathVariable String key, Principal principal) {
        User owner = userService.requireByEmail(principal.getName());
        ChatConversation chat = chatService.buildFor(owner, key);
        if (chat == null) {
            throw new NoSuchElementException("No conversation " + key);
        }
        return ConversationDetail.from(chat);
    }

    @PostMapping("/{key}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void markRead(@PathVariable String key, Principal principal) {
        User owner = userService.requireByEmail(principal.getName());
        chatService.markRead(owner, null, key);
    }

    private static ChatFilter parseFilter(String raw) {
        try {
            return ChatFilter.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown filter: " + raw);
        }
    }
}
