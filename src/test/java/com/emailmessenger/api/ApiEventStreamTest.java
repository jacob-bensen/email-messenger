package com.emailmessenger.api;

import com.emailmessenger.email.NewMailEvent;
import com.emailmessenger.repository.ApiTokenRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** docs/API.md §5.24. */
class ApiEventStreamTest {

    private final ApiTokenRepository tokens = mock(ApiTokenRepository.class);
    private final List<SseEmitter> created = new ArrayList<>();
    private final ApiEventStream stream = new ApiEventStream(tokens, new ObjectMapper(),
            Clock.fixed(Instant.parse("2026-11-25T10:00:00Z"), ZoneOffset.UTC)) {
        @Override
        protected SseEmitter createEmitter() {
            SseEmitter spy = Mockito.spy(new SseEmitter(1000L));
            created.add(spy);
            return spy;
        }
    };

    private static ApiPrincipal principal(long userId, long tokenId) {
        return new ApiPrincipal(userId, "u" + userId + "@example.com", tokenId);
    }

    @Test
    void eventCarriesTheChangedKeysSorted() throws IOException {
        stream.subscribe(principal(1, 10));

        stream.onNewMail(new NewMailEvent(1L, 3, Set.of("bbb", "aaa")));

        ArgumentCaptor<SseEmitter.SseEventBuilder> sent = ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
        verify(created.get(0)).send(sent.capture());
        String wire = sent.getValue().build().stream()
                .map(d -> String.valueOf(d.getData()))
                .reduce("", String::concat);
        assertThat(wire).contains("event:conversations-changed");
        assertThat(wire).contains("{\"imported\":3,\"keys\":[\"aaa\",\"bbb\"]}");
    }

    @Test
    void everyDeviceOfTheUserIsNotifiedAndOtherUsersAreNot() throws IOException {
        stream.subscribe(principal(1, 10));
        stream.subscribe(principal(1, 11));
        stream.subscribe(principal(2, 20));

        stream.onNewMail(new NewMailEvent(1L, 1, Set.of("k")));

        verify(created.get(0)).send(any(SseEmitter.SseEventBuilder.class));
        verify(created.get(1)).send(any(SseEmitter.SseEventBuilder.class));
        verify(created.get(2), never()).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void deadStreamIsDroppedOnSendFailure() throws IOException {
        stream.subscribe(principal(1, 10));
        doThrow(new IOException("broken pipe")).when(created.get(0)).send(any(SseEmitter.SseEventBuilder.class));

        stream.onNewMail(new NewMailEvent(1L, 1, Set.of()));

        assertThat(stream.activeCount(1)).isZero();
    }

    @Test
    void keepaliveClosesStreamsWhoseTokenWasRevoked() throws IOException {
        stream.subscribe(principal(1, 10));
        stream.subscribe(principal(1, 11));
        when(tokens.findActiveIdsIn(anyCollection(), any())).thenReturn(List.of(11L));

        stream.keepalive();

        assertThat(stream.activeCount(1)).isEqualTo(1);
        verify(created.get(0)).complete();
        verify(created.get(0), never()).send(any(SseEmitter.SseEventBuilder.class));
        verify(created.get(1)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void keepaliveWithNoStreamsSkipsTheTokenQuery() {
        stream.keepalive();

        verifyNoInteractions(tokens);
    }
}
