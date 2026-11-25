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
import static org.mockito.ArgumentMatchers.any;
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

    private static final ApiDtos.ConversationsChanged CHANGES = new ApiDtos.ConversationsChanged(
            List.of(), new ApiDtos.CountsResponse(1, 1, 0, 0), false);

    @Test
    void buildsTheEventOnceAndSendsItToEveryStreamOfThatUser() throws IOException {
        when(conversations.changes(1L, Set.of("k1"))).thenReturn(Optional.of(CHANGES));
        stream.subscribe(1L);
        stream.subscribe(1L);

        stream.onNewMail(new NewMailEvent(1L, 2, Set.of("k1")));

        verify(conversations, times(1)).changes(1L, Set.of("k1"));
        verify(created.get(0)).send(any(SseEmitter.SseEventBuilder.class));
        verify(created.get(1)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void doesNoWorkForAUserWithNoStreamOpen() throws IOException {
        stream.subscribe(2L);

        stream.onNewMail(new NewMailEvent(1L, 1, Set.of("k1")));

        verify(conversations, never()).changes(anyLong(), anySet());
        verify(created.get(0), never()).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void skipsAnEventWithNoConversationKeys() throws IOException {
        stream.subscribe(1L);

        stream.onNewMail(new NewMailEvent(1L, 1, Set.of()));

        verify(conversations, never()).changes(anyLong(), anySet());
        verify(created.get(0), never()).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void aFailureBuildingTheEventIsSwallowed() throws IOException {
        when(conversations.changes(anyLong(), anySet())).thenThrow(new IllegalStateException("db down"));
        stream.subscribe(1L);

        stream.onNewMail(new NewMailEvent(1L, 1, Set.of("k1")));

        verify(created.get(0), never()).send(any(SseEmitter.SseEventBuilder.class));
        assertThat(stream.activeCount(1L)).isEqualTo(1);
    }

    @Test
    void deadStreamsAreDroppedOnSendAndOnKeepalive() throws IOException {
        when(conversations.changes(anyLong(), anySet())).thenReturn(Optional.of(CHANGES));
        stream.subscribe(1L);
        stream.subscribe(1L);
        doThrow(new IOException("broken pipe")).when(created.get(0)).send(any(SseEmitter.SseEventBuilder.class));

        stream.onNewMail(new NewMailEvent(1L, 1, Set.of("k1")));
        assertThat(stream.activeCount(1L)).isEqualTo(1);

        doThrow(new IOException("gone")).when(created.get(1)).send(any(SseEmitter.SseEventBuilder.class));
        stream.keepalive();
        assertThat(stream.activeCount(1L)).isZero();
    }
}
