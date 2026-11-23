package com.emailmessenger.api;

import com.emailmessenger.email.NewMailEvent;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.PlatformTransactionManager;
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
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiEventStreamTest {

    private final ApiEventPayloads payloads = mock(ApiEventPayloads.class);
    private final ApiTokenService tokens = mock(ApiTokenService.class);
    private final List<SseEmitter> created = new ArrayList<>();

    // Spied emitters: SseEmitter buffers sends until a real async request
    // attaches, so the spy is what makes them observable.
    private final ApiEventStream stream = new ApiEventStream(payloads, tokens, mock(PlatformTransactionManager.class)) {
        @Override
        protected SseEmitter createEmitter() {
            SseEmitter spy = Mockito.spy(new SseEmitter(1000L));
            created.add(spy);
            return spy;
        }
    };

    private static final ApiDtos.ConversationsEvent PAYLOAD = new ApiDtos.ConversationsEvent(
            1, List.of(), false, new ApiDtos.CountsResponse(1, 1, 1, 0));

    @Test
    void subscribingSendsReady() throws IOException {
        stream.subscribe(principal(1L, 10L));

        verify(created.get(0)).send(any(SseEmitter.SseEventBuilder.class));
        assertThat(stream.activeCount(1L)).isEqualTo(1);
    }

    @Test
    void newMailBuildsThePayloadOnceAndSendsItToEveryStreamOfThatUser() throws IOException {
        when(payloads.conversationsChanged(1L, 2, Set.of("k1"))).thenReturn(Optional.of(PAYLOAD));
        stream.subscribe(principal(1L, 10L));
        stream.subscribe(principal(1L, 11L)); // a second device
        stream.subscribe(principal(2L, 20L));

        stream.onNewMail(new NewMailEvent(1L, 2, Set.of("k1")));

        verify(payloads, times(1)).conversationsChanged(1L, 2, Set.of("k1"));
        verify(created.get(0), times(2)).send(any(SseEmitter.SseEventBuilder.class)); // ready + event
        verify(created.get(1), times(2)).send(any(SseEmitter.SseEventBuilder.class));
        verify(created.get(2), times(1)).send(any(SseEmitter.SseEventBuilder.class)); // ready only
    }

    @Test
    void noStreamsMeansNoPayloadWork() {
        stream.onNewMail(new NewMailEvent(1L, 2, Set.of("k1")));

        verify(payloads, never()).conversationsChanged(anyLong(), anyInt(), anySet());
    }

    @Test
    void aFailingPayloadDoesNotPropagateToThePoll() {
        when(payloads.conversationsChanged(anyLong(), anyInt(), anySet())).thenThrow(new IllegalStateException("db"));
        stream.subscribe(principal(1L, 10L));

        stream.onNewMail(new NewMailEvent(1L, 1, Set.of("k1")));

        assertThat(stream.activeCount(1L)).isEqualTo(1);
    }

    @Test
    void deadStreamIsDroppedOnSendFailure() throws IOException {
        when(payloads.conversationsChanged(anyLong(), anyInt(), anySet())).thenReturn(Optional.of(PAYLOAD));
        stream.subscribe(principal(1L, 10L));
        doThrow(new IOException("broken pipe")).when(created.get(0)).send(any(SseEmitter.SseEventBuilder.class));

        stream.onNewMail(new NewMailEvent(1L, 1, Set.of("k1")));

        assertThat(stream.activeCount(1L)).isZero();
    }

    @Test
    void keepaliveClosesStreamsOfRevokedTokensAndPingsTheRest() throws IOException {
        when(tokens.isActive(10L)).thenReturn(false);
        when(tokens.isActive(11L)).thenReturn(true);
        stream.subscribe(principal(1L, 10L));
        stream.subscribe(principal(1L, 11L));

        stream.keepalive();

        assertThat(stream.activeCount(1L)).isEqualTo(1);
        verify(created.get(0)).complete();
        verify(created.get(0), times(1)).send(any(SseEmitter.SseEventBuilder.class)); // ready only
        verify(created.get(1), times(2)).send(any(SseEmitter.SseEventBuilder.class)); // ready + ping
    }

    private static ApiPrincipal principal(long userId, long tokenId) {
        return new ApiPrincipal(userId, "u" + userId + "@example.com", tokenId);
    }
}
