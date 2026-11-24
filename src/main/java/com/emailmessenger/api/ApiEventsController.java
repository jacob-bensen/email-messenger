package com.emailmessenger.api;

import com.emailmessenger.web.ChatEventStream;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** API.md §5.24: the app's live new-mail stream, shared with the website's. */
@RestController
class ApiEventsController {

    private final ChatEventStream eventStream;

    ApiEventsController(ChatEventStream eventStream) {
        this.eventStream = eventStream;
    }

    @GetMapping("/api/v1/events")
    SseEmitter events(@AuthenticationPrincipal ApiPrincipal principal) {
        return eventStream.subscribe(principal.userId());
    }
}
