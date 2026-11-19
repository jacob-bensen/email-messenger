package com.emailmessenger.api;

import com.emailmessenger.email.NewMailEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
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

    private final List<SseEmitter> created = new ArrayList<>();
    private final ApiConversationService conversations = mock(ApiConversationService.class);

    private final ApiEventStream stream = new ApiEventStream(conversations, new ObjectMapper()) {
        @Override
        protected SseEmitter createEmitter() {
            SseEmitter spy = Mockito.spy(new SseEmitter(1000L));
            created.add(spy);
            return spy;
        }
    };

    private static final ApiDtos.ConversationsChanged PAYLOAD = new ApiDtos.ConversationsChanged(
            1, List.of(), false, new ApiDtos.CountsResponse(1, 1, 1, 0));

    @Test
    void buildsThePayloadOnceAndSendsItToEveryStreamOfThatUser() throws IOException {
        when(conversations.changes(1L, 3, Set.of("k1"))).thenReturn(PAYLOAD);
        stream.subscribe(1L);
        stream.subscribe(1L);

        stream.onNewMail(new NewMailEvent(1L, 3, Set.of("k1")));

        verify(conversations, times(1)).changes(1L, 3, Set.of("k1"));
        verify(created.get(0)).send(any(SseEmitter.SseEventBuilder.class));
        verify(created.get(1)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void noStreamMeansNoPayloadIsBuilt() {
        stream.subscribe(2L);

        stream.onNewMail(new NewMailEvent(1L, 1, Set.of("k1")));

        verify(conversations, never()).changes(anyLong(), anyInt(), anySet());
    }

    @Test
    void deadStreamIsDroppedOnSendFailure() throws IOException {
        when(conversations.changes(anyLong(), anyInt(), anySet())).thenReturn(PAYLOAD);
        stream.subscribe(1L);
        doThrow(new IOException("broken pipe")).when(created.get(0)).send(any(SseEmitter.SseEventBuilder.class));

        stream.onNewMail(new NewMailEvent(1L, 1, Set.of("k1")));

        assertThat(stream.activeCount(1L)).isZero();
    }

    @Test
    void payloadFailureSendsNothing() throws IOException {
        when(conversations.changes(anyLong(), anyInt(), anySet())).thenThrow(new IllegalStateException("gone"));
        stream.subscribe(1L);

        stream.onNewMail(new NewMailEvent(1L, 1, Set.of("k1")));

        verify(created.get(0), never()).send(any(SseEmitter.SseEventBuilder.class));
        assertThat(stream.activeCount(1L)).isEqualTo(1);
    }
}
