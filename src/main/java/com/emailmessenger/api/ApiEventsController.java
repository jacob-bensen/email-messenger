package com.emailmessenger.api;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** docs/API.md §5.24. */
@RestController
class ApiEventsController {

    private final ApiEventStream events;

    ApiEventsController(ApiEventStream events) {
        this.events = events;
    }

    @GetMapping(path = "/api/v1/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter events(@AuthenticationPrincipal ApiPrincipal principal) {
        return events.subscribe(principal.userId());
    }
}
