package com.emailmessenger.web;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.User;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.security.Principal;

/**
 * SSE stream the chats view subscribes to for live "new mail arrived"
 * signals. The page reacts by refreshing the rail/pane in place — see
 * {@code fragments/live-refresh.html}.
 */
@Controller
class ChatEventsController {

    private final ChatEventStream eventStream;
    private final UserService userService;

    ChatEventsController(ChatEventStream eventStream, UserService userService) {
        this.eventStream = eventStream;
        this.userService = userService;
    }

    @GetMapping(path = "/chats/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter events(Principal principal) {
        User user = userService.requireByEmail(principal.getName());
        return eventStream.subscribe(user.getId());
    }
}
