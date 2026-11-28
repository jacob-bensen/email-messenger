package com.emailmessenger.web;

import com.emailmessenger.email.MailboxesChangedEvent;
import com.emailmessenger.email.NewMailEvent;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ChatEventStreamTest {

    private final List<SseEmitter> created = new ArrayList<>();

    // Spy the emitters so the sends (which SseEmitter buffers until a real
    // async request attaches) can be observed.
    private final ChatEventStream stream = new ChatEventStream() {
        @Override
        protected SseEmitter createEmitter() {
            SseEmitter spy = Mockito.spy(new SseEmitter(1000L));
            created.add(spy);
            return spy;
        }
    };

    @Test
    void newMailEventIsSentToEverySubscriberOfThatUser() throws IOException {
        stream.subscribe(1L);
        stream.subscribe(1L); // second tab

        stream.onNewMail(new NewMailEvent(1L, 3, List.of()));

        verify(created.get(0)).send(any(SseEmitter.SseEventBuilder.class));
        verify(created.get(1)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void otherUsersAreNotNotified() throws IOException {
        stream.subscribe(1L);
        stream.subscribe(2L);

        stream.onNewMail(new NewMailEvent(1L, 1, List.of()));

        verify(created.get(0)).send(any(SseEmitter.SseEventBuilder.class));
        verify(created.get(1), never()).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void deadEmitterIsDroppedOnSendFailure() throws IOException {
        stream.subscribe(1L);
        doThrow(new IOException("broken pipe"))
                .when(created.get(0)).send(any(SseEmitter.SseEventBuilder.class));

        stream.onNewMail(new NewMailEvent(1L, 1, List.of()));

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
        verify(created.get(1)).send(any(SseEmitter.SseEventBuilder.class));

        // The survivor still gets real events afterwards.
        stream.onNewMail(new NewMailEvent(1L, 2, List.of()));
        verify(created.get(1), times(2)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void eventForUserWithNoSubscribersIsANoOp() {
        stream.onNewMail(new NewMailEvent(99L, 1, List.of()));
        assertThat(stream.activeCount(99L)).isZero();
    }

    @Test
    void sendsAndReadsGoOutUnderTheirOwnEventNames() throws IOException {
        stream.subscribe(1L);

        stream.onConversationChanged(new ConversationChangedEvent(
                1L, ConversationChangedEvent.Kind.SENT, List.of("aaaa")));
        stream.onConversationChanged(new ConversationChangedEvent(
                1L, ConversationChangedEvent.Kind.READ, List.of("aaaa")));

        ArgumentCaptor<SseEmitter.SseEventBuilder> sent = ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
        verify(created.get(0), times(2)).send(sent.capture());
        assertThat(rendered(sent.getAllValues().get(0)))
                .startsWith("event:message-sent\n").contains("data:{\"conversationKeys\":[\"aaaa\"]}");
        assertThat(rendered(sent.getAllValues().get(1)))
                .startsWith("event:conversation-read\n").contains("data:{\"conversationKeys\":[\"aaaa\"]}");
    }

    @Test
    void conversationChangesReachOnlyThatUser() throws IOException {
        stream.subscribe(1L);
        stream.subscribe(2L);

        stream.onConversationChanged(new ConversationChangedEvent(
                2L, ConversationChangedEvent.Kind.READ, List.of("bbbb")));

        verify(created.get(0), never()).send(any(SseEmitter.SseEventBuilder.class));
        verify(created.get(1)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void mailboxChangesGoOutAsMailboxesChangedWithSortedIds() throws IOException {
        stream.subscribe(1L);
        stream.subscribe(2L);

        stream.onMailboxesChanged(new MailboxesChangedEvent(1L, List.of(7L, 3L, 7L)));

        ArgumentCaptor<SseEmitter.SseEventBuilder> sent = ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
        verify(created.get(0)).send(sent.capture());
        assertThat(rendered(sent.getValue()))
                .startsWith("event:mailboxes-changed\n").contains("data:{\"mailboxIds\":[3,7]}");
        verify(created.get(1), never()).send(any(SseEmitter.SseEventBuilder.class));
    }

    private static String rendered(SseEmitter.SseEventBuilder event) {
        StringBuilder out = new StringBuilder();
        event.build().forEach(part -> out.append(part.getData()));
        return out.toString();
    }

    @Test
    void newMailPayloadCarriesCountAndConversationKeys() {
        assertThat(ChatEventStream.newMailData(new NewMailEvent(1L, 2, List.of("aaaa", "bbbb"))))
                .isEqualTo("{\"imported\":2,\"conversationKeys\":[\"aaaa\",\"bbbb\"]}");
    }
}
