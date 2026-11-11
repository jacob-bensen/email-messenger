package com.emailmessenger.api;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.MailAccountService;
import com.emailmessenger.email.MailboxPollingService;
import com.emailmessenger.web.ConversationListService;
import com.emailmessenger.web.ConversationListService.ChatFilter;
import com.emailmessenger.web.MailboxScope;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

/**
 * The chats list for the mobile client — the JSON twin of {@code GET /chats}.
 * Scope resolution, filtering, ordering, previews, and counts all come from
 * the same {@link ConversationListService} / {@link MailboxScope} the
 * Thymeleaf workspace uses.
 */
@RestController
@RequestMapping("/api/v1/conversations")
class ConversationApiController {

    static final int PAGE_SIZE = 30;

    private final ConversationListService conversationListService;
    private final MailAccountService mailAccountService;
    private final MailboxPollingService mailboxPollingService;
    private final UserService userService;

    ConversationApiController(ConversationListService conversationListService,
                              MailAccountService mailAccountService,
                              MailboxPollingService mailboxPollingService,
                              UserService userService) {
        this.conversationListService = conversationListService;
        this.mailAccountService = mailAccountService;
        this.mailboxPollingService = mailboxPollingService;
        this.userService = userService;
    }

    @GetMapping
    ConversationPageDto list(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(name = "q", required = false) String query,
                             @RequestParam(name = "filter", required = false) String filter,
                             @RequestParam(name = "mailboxId", required = false) String mailboxId,
                             Principal principal) {
        User owner = userService.requireByEmail(principal.getName());
        mailboxPollingService.refreshDueForUserAsync(owner.getId());
        List<MailAccount> accounts = mailAccountService.list(owner);
        String scope = MailboxScope.address(MailboxScope.select(accounts, mailboxId), accounts);
        return ConversationPageDto.of(
                conversationListService.list(owner, scope, query, ChatFilter.parse(filter),
                        PageRequest.of(Math.max(0, page), PAGE_SIZE)),
                conversationListService.counts(owner, scope));
    }
}
