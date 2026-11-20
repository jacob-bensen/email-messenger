package com.emailmessenger.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
class ApiConversationController {

    static final String IDEMPOTENCY_KEY = "Idempotency-Key";

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

    @PostMapping("/conversations/{key}/messages")
    ResponseEntity<ApiDtos.SendResponse> send(@AuthenticationPrincipal ApiPrincipal principal,
                                              @PathVariable String key,
                                              @RequestParam(required = false) Long mailboxId,
                                              @RequestHeader(name = IDEMPOTENCY_KEY, required = false)
                                              String idempotencyKey,
                                              @RequestBody ApiDtos.SendMessageRequest request) {
        return created(conversations.send(principal, key, mailboxId, request, idempotencyKey));
    }

    @PostMapping("/conversations")
    ResponseEntity<ApiDtos.SendResponse> start(@AuthenticationPrincipal ApiPrincipal principal,
                                               @RequestParam(required = false) Long mailboxId,
                                               @RequestHeader(name = IDEMPOTENCY_KEY, required = false)
                                               String idempotencyKey,
                                               @RequestBody ApiDtos.NewConversationRequest request) {
        return created(conversations.start(principal, mailboxId, request, idempotencyKey));
    }

    private static ResponseEntity<ApiDtos.SendResponse> created(ApiSendIdempotency.Outcome outcome) {
        ResponseEntity.BodyBuilder response = ResponseEntity.status(HttpStatus.CREATED);
        if (outcome.replayed()) {
            response.header(ApiSendIdempotency.REPLAYED_HEADER, "true");
        }
        return response.body(outcome.response());
    }
}
