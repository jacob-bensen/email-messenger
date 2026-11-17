package com.emailmessenger.api;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
class ApiConversationController {

    private final ApiConversationService conversations;

    ApiConversationController(ApiConversationService conversations) {
        this.conversations = conversations;
    }

    @GetMapping("/conversations")
    ApiDtos.ConversationPage list(@AuthenticationPrincipal ApiPrincipal principal,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "" + ApiConversationService.DEFAULT_PAGE_SIZE) int size,
                                  @RequestParam(name = "q", required = false) String query,
                                  @RequestParam(required = false) Long mailboxId,
                                  @RequestParam(required = false) String filter) {
        return conversations.list(principal, page, size, query, mailboxId, filter);
    }

    @GetMapping("/conversations/{key}")
    ApiDtos.ConversationDetail get(@AuthenticationPrincipal ApiPrincipal principal,
                                   @PathVariable String key,
                                   @RequestParam(required = false) Long mailboxId) {
        return conversations.get(principal, key, mailboxId);
    }

    @PostMapping("/conversations/{key}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void markRead(@AuthenticationPrincipal ApiPrincipal principal,
                  @PathVariable String key,
                  @RequestParam(required = false) Long mailboxId) {
        conversations.markRead(principal, key, mailboxId);
    }
}
