package com.emailmessenger.web;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatEventsControllerTest {

    private final ChatEventStream stream = mock(ChatEventStream.class);
    private final UserService userService = mock(UserService.class);
    private final ChatEventsController controller = new ChatEventsController(stream, userService);

    @Test
    void subscribesTheAuthenticatedUser() {
        Principal principal = () -> "ada@example.com";
        User user = mock(User.class);
        when(user.getId()).thenReturn(4L);
        when(userService.requireByEmail("ada@example.com")).thenReturn(user);
        SseEmitter emitter = new SseEmitter();
        when(stream.subscribe(4L)).thenReturn(emitter);

        assertThat(controller.events(principal)).isSameAs(emitter);
    }
}
