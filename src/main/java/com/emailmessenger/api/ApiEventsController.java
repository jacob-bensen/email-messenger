package com.emailmessenger.api;

import com.emailmessenger.web.ChatEventStream;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** docs/API.md §5.24 — the same fan-out as the website's {@code /chats/events}. */
@RestController
class ApiEventsController {

    private final ChatEventStream eventStream;

    ApiEventsController(ChatEventStream eventStream) {
        this.eventStream = eventStream;
    }

    @GetMapping(path = "/api/v1/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter events(@AuthenticationPrincipal ApiPrincipal principal) {
        return eventStream.subscribe(principal.userId());
    }
}
