package com.emailmessenger.api;

import com.emailmessenger.api.ConversationDtos.DetailDto;
import com.emailmessenger.api.ConversationDtos.PageDto;
import com.emailmessenger.api.ConversationDtos.SummaryDto;
import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.MailAccountService;
import com.emailmessenger.email.MailboxPollingService;
import com.emailmessenger.service.ChatConversation;
import com.emailmessenger.web.ChatService;
import com.emailmessenger.web.ConversationListService;
import com.emailmessenger.web.ConversationListService.ChatFilter;
import com.emailmessenger.web.MailboxScope;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

/**
 * The chats list and a single conversation timeline as JSON. Everything here is
 * the same {@link ConversationListService} / {@link ChatService} calls the
 * Thymeleaf workspace at {@code /chats} makes — this class only maps the view
 * models onto the wire shapes in docs/API.md.
 */
@RestController
@RequestMapping("/api/v1/conversations")
class ApiConversationController {

    static final int DEFAULT_PAGE_SIZE = 30;
    static final int MAX_PAGE_SIZE = 100;

    private final ConversationListService conversationListService;
    private final ChatService chatService;
    private final UserService userService;
    private final MailAccountService mailAccountService;
    private final MailboxPollingService mailboxPollingService;

    ApiConversationController(ConversationListService conversationListService,
                              ChatService chatService,
                              UserService userService,
                              MailAccountService mailAccountService,
                              MailboxPollingService mailboxPollingService) {
        this.conversationListService = conversationListService;
        this.chatService = chatService;
        this.userService = userService;
        this.mailAccountService = mailAccountService;
        this.mailboxPollingService = mailboxPollingService;
    }

    @GetMapping
    PageDto<SummaryDto> list(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size,
                             @RequestParam(name = "q", required = false) String query,
                             @RequestParam(name = "filter", required = false) String filter,
                             @RequestParam(name = "mailbox", required = false) String mailbox,
                             Principal principal) {
        User owner = userService.requireByEmail(principal.getName());
        // Same non-blocking refresh of due mailboxes that opening /chats triggers.
        mailboxPollingService.refreshDueForUserAsync(owner.getId());
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), MAX_PAGE_SIZE));
        return PageDto.ofSummaries(conversationListService.list(
                owner, scope(owner, mailbox), query, ChatFilter.parse(filter), pageable));
    }

    /** Read-only: unlike the web pane, fetching a conversation does not mark it read. */
    @GetMapping("/{key}")
    DetailDto detail(@PathVariable String key,
                     @RequestParam(name = "mailbox", required = false) String mailbox,
                     Principal principal) {
        User owner = userService.requireByEmail(principal.getName());
        ChatConversation conversation = chatService.buildFor(owner, scope(owner, mailbox), key);
        if (conversation == null) {
            throw ApiException.notFound("That conversation doesn't exist.");
        }
        return DetailDto.from(conversation);
    }

    @PostMapping("/{key}/read")
    ResponseEntity<Void> markRead(@PathVariable String key,
                                  @RequestParam(name = "mailbox", required = false) String mailbox,
                                  Principal principal) {
        User owner = userService.requireByEmail(principal.getName());
        if (!chatService.markRead(owner, scope(owner, mailbox), key)) {
            throw ApiException.notFound("That conversation doesn't exist.");
        }
        return ResponseEntity.noContent().build();
    }

    private String scope(User owner, String mailboxParam) {
        List<MailAccount> accounts = mailAccountService.list(owner);
        return MailboxScope.address(accounts, mailboxParam);
    }
}
