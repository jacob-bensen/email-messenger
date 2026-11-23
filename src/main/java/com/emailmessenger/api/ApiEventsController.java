package com.emailmessenger.api;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** docs/API.md §5.21. */
@RestController
class ApiEventsController {

    private final ApiEventStream stream;

    ApiEventsController(ApiEventStream stream) {
        this.stream = stream;
    }

    @GetMapping(path = "/api/v1/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter events(@AuthenticationPrincipal ApiPrincipal principal) {
        return stream.subscribe(principal);
    }
}
