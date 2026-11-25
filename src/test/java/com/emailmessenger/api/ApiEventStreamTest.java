package com.emailmessenger.api;

import com.emailmessenger.email.NewMailEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiEventStreamTest {

    private final ApiConversationService conversations = mock(ApiConversationService.class);
    private final List<SseEmitter> created = new ArrayList<>();
    private final ApiEventStream stream = new ApiEventStream(conversations, new ObjectMapper()) {
        @Override
        protected SseEmitter createEmitter() {
            SseEmitter spy = Mockito.spy(new SseEmitter(1000L));
            created.add(spy);
            return spy;
        }
    };

    private static final ApiDtos.ConversationsChanged CHANGED = new ApiDtos.ConversationsChanged(
            1, List.of(), new ApiDtos.CountsResponse(1, 1, 0, 0));

    @Test
    void buildsTheEventOncePerUserAndSendsItToEveryDevice() throws IOException {
        when(conversations.changed(1L, 2, Set.of("k"))).thenReturn(Optional.of(CHANGED));
        stream.subscribe(1L);
        stream.subscribe(1L);
        stream.subscribe(2L);

        stream.onNewMail(new NewMailEvent(1L, 2, Set.of("k")));

        verify(conversations, times(1)).changed(1L, 2, Set.of("k"));
        verify(created.get(0)).send(any(SseEmitter.SseEventBuilder.class));
        verify(created.get(1)).send(any(SseEmitter.SseEventBuilder.class));
        verify(created.get(2), never()).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void noSubscriberMeansNoQuery() {
        stream.onNewMail(new NewMailEvent(1L, 1, Set.of("k")));

        verify(conversations, never()).changed(anyLong(), anyInt(), anySet());
    }

    @Test
    void aFailureToBuildTheEventDoesNotReachThePoll() throws IOException {
        when(conversations.changed(anyLong(), anyInt(), anySet())).thenThrow(new IllegalStateException("db down"));
        stream.subscribe(1L);

        assertThatCode(() -> stream.onNewMail(new NewMailEvent(1L, 1, Set.of("k")))).doesNotThrowAnyException();
        verify(created.get(0), never()).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void aDeletedUserGetsNothing() throws IOException {
        when(conversations.changed(anyLong(), anyInt(), anySet())).thenReturn(Optional.empty());
        stream.subscribe(1L);

        stream.onNewMail(new NewMailEvent(1L, 1, Set.of("k")));

        verify(created.get(0), never()).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void deadEmittersAreDropped() throws IOException {
        when(conversations.changed(anyLong(), anyInt(), anySet())).thenReturn(Optional.of(CHANGED));
        stream.subscribe(1L);
        stream.subscribe(1L);
        doThrow(new IOException("broken pipe")).when(created.get(0)).send(any(SseEmitter.SseEventBuilder.class));

        stream.onNewMail(new NewMailEvent(1L, 1, Set.of("k")));

        assertThat(stream.activeCount(1L)).isEqualTo(1);
    }
}
