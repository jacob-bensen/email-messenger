package com.emailmessenger.web;

import com.emailmessenger.email.NewMailEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ChatEventStreamTest {

    private final List<SseEmitter> created = new ArrayList<>();

    // Spy the emitters so the sends (which SseEmitter buffers until a real
    // async request attaches) can be observed.
    private final ChatEventStream stream = new ChatEventStream(new ObjectMapper()) {
        @Override
        protected SseEmitter createEmitter() {
            SseEmitter spy = Mockito.spy(new SseEmitter(1000L));
            created.add(spy);
            return spy;
        }
    };

    // Each subscriber gets the opening ":ok" comment first, so counts below
    // include that one send.

    @Test
    void newMailEventCarriesTheCountAndConversationKeysAsJson() throws IOException {
        stream.subscribe(1L);

        stream.onNewMail(new NewMailEvent(1L, 2, List.of("k1", "k2")));

        ArgumentCaptor<SseEmitter.SseEventBuilder> sent =
                ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
        verify(created.get(0), times(2)).send(sent.capture());
        String opening = render(sent.getAllValues().get(0));
        String event = render(sent.getAllValues().get(1));
        assertThat(opening).startsWith(":ok\n");
        assertThat(event).startsWith(
                "event:new-mail\ndata:{\"imported\":2,\"conversationKeys\":[\"k1\",\"k2\"]}\n");
    }

    private static String render(SseEmitter.SseEventBuilder builder) {
        StringBuilder out = new StringBuilder();
        builder.build().forEach(part -> out.append(part.getData()));
        return out.toString();
    }

    @Test
    void newMailEventIsSentToEverySubscriberOfThatUser() throws IOException {
        stream.subscribe(1L);
        stream.subscribe(1L); // second tab

        stream.onNewMail(new NewMailEvent(1L, 3, List.of("k")));

        verify(created.get(0), times(2)).send(any(SseEmitter.SseEventBuilder.class));
        verify(created.get(1), times(2)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void otherUsersAreNotNotified() throws IOException {
        stream.subscribe(1L);
        stream.subscribe(2L);

        stream.onNewMail(new NewMailEvent(1L, 1, List.of("k")));

        verify(created.get(0), times(2)).send(any(SseEmitter.SseEventBuilder.class));
        verify(created.get(1), times(1)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void deadEmitterIsDroppedOnSendFailure() throws IOException {
        stream.subscribe(1L);
        doThrow(new IOException("broken pipe"))
                .when(created.get(0)).send(any(SseEmitter.SseEventBuilder.class));

        stream.onNewMail(new NewMailEvent(1L, 1, List.of("k")));

        assertThat(stream.activeCount(1L)).isZero();
    }

    @Test
    void keepaliveReapsDeadEmittersAndPingsLiveOnes() throws IOException {
        stream.subscribe(1L);
        stream.subscribe(1L);
        doThrow(new IOException("gone"))
                .when(created.get(0)).send(any(SseEmitter.SseEventBuilder.class));

        stream.keepalive();

        assertThat(stream.activeCount(1L)).isEqualTo(1);
        verify(created.get(1), times(2)).send(any(SseEmitter.SseEventBuilder.class));

        // The survivor still gets real events afterwards.
        stream.onNewMail(new NewMailEvent(1L, 2, List.of("k")));
        verify(created.get(1), times(3)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void eventForUserWithNoSubscribersIsANoOp() {
        stream.onNewMail(new NewMailEvent(99L, 1, List.of("k")));
        assertThat(stream.activeCount(99L)).isZero();
    }
}
