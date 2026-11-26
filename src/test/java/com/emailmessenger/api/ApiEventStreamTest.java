package com.emailmessenger.api;

import com.emailmessenger.email.NewMailEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiEventStreamTest {

    private final ApiConversationService conversations = mock(ApiConversationService.class);
    private final List<SseEmitter> created = new ArrayList<>();

    private final ApiEventStream stream = new ApiEventStream(conversations) {
        @Override
        protected SseEmitter createEmitter() {
            SseEmitter spy = Mockito.spy(new SseEmitter(1000L));
            created.add(spy);
            return spy;
        }
    };

    private final ApiDtos.NewMailUpdate update = new ApiDtos.NewMailUpdate(1, List.of(), true,
            new ApiDtos.CountsResponse(1, 1, 1, 0));

    @Test
    void noStreamOpenBuildsNothing() {
        stream.onNewMail(new NewMailEvent(1L, 2, Set.of("k")));

        verify(conversations, never()).newMailUpdate(anyLong(), anyInt(), any());
    }

    @Test
    void sendsTheUpdateAsJsonToEveryStreamOfThatUserOnly() throws IOException {
        when(conversations.newMailUpdate(1L, 2, Set.of("k"))).thenReturn(Optional.of(update));
        stream.subscribe(1L);
        stream.subscribe(1L);
        stream.subscribe(2L);

        stream.onNewMail(new NewMailEvent(1L, 2, Set.of("k")));

        for (SseEmitter emitter : created.subList(0, 2)) {
            ArgumentCaptor<SseEmitter.SseEventBuilder> sent = ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
            verify(emitter).send(sent.capture());
            var parts = List.copyOf(sent.getValue().build());
            assertThat(parts.get(0).getData().toString()).contains("event:new-mail");
            assertThat(parts.get(1).getData()).isSameAs(update);
            assertThat(parts.get(1).getMediaType()).isEqualTo(MediaType.APPLICATION_JSON);
        }
        verify(created.get(2), never()).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void aFailureBuildingTheUpdateDoesNotEscapeTheListener() throws IOException {
        when(conversations.newMailUpdate(anyLong(), anyInt(), any())).thenThrow(new IllegalStateException("db"));
        stream.subscribe(1L);

        stream.onNewMail(new NewMailEvent(1L, 1, Set.of("k")));

        verify(created.get(0), never()).send(any(SseEmitter.SseEventBuilder.class));
        assertThat(stream.activeCount(1L)).isEqualTo(1);
    }

    @Test
    void keepalivePingsAndDropsDeadStreams() throws IOException {
        stream.subscribe(1L);
        stream.subscribe(1L);
        Mockito.doThrow(new IOException("gone")).when(created.get(0)).send(any(SseEmitter.SseEventBuilder.class));

        stream.keepalive();

        assertThat(stream.activeCount(1L)).isEqualTo(1);
        verify(created.get(1)).send(any(SseEmitter.SseEventBuilder.class));
    }
}
