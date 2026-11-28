package com.emailmessenger.service;

import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.Message;
import com.emailmessenger.domain.Participant;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.MessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReplyThreadingTest {

    @Mock MessageRepository messages;

    private final EmailThread thread = thread();
    private final Participant ada = new Participant("ada@example.com", "Ada");

    @Test
    void latestInAnswersTheThreadsLastMessage() {
        Message first = message("<a@x>", null);
        Message last = message("<b@x>", "<a@x>");
        when(messages.findByThreadIdOrderBySentAtAsc(7L)).thenReturn(List.of(first, last));

        assertThat(new ReplyThreading(messages).latestIn(7L))
                .contains(new ReplyService.Threading(7L, "Lunch", "<b@x>", "<a@x> <b@x>"));
    }

    @Test
    void latestInIsEmptyForAThreadWithoutMessages() {
        when(messages.findByThreadIdOrderBySentAtAsc(7L)).thenReturn(List.of());

        assertThat(new ReplyThreading(messages).latestIn(7L)).isEmpty();
    }

    @Test
    void answeringUsesTheMessagesOwnHeaders() {
        when(messages.findById(5L)).thenReturn(Optional.of(message("<a@x>", null)));

        assertThat(new ReplyThreading(messages).answering(5L))
                .contains(new ReplyService.Threading(7L, "Lunch", "<a@x>", "<a@x>"));
    }

    @Test
    void aMessageWithoutMessageIdKeepsTheThreadButNoHeaders() {
        when(messages.findById(5L)).thenReturn(Optional.of(message(null, "<a@x>")));

        assertThat(new ReplyThreading(messages).answering(5L))
                .contains(new ReplyService.Threading(7L, "Lunch", null, null));
    }

    private Message message(String id, String inReplyTo) {
        Message m = new Message(thread, ada, "Lunch", "body", null, LocalDateTime.of(2026, 11, 16, 9, 0));
        m.setMessageIdHeader(id);
        m.setInReplyTo(inReplyTo);
        return m;
    }

    private static EmailThread thread() {
        EmailThread t = new EmailThread(new User("owner@test.com", "h", null), "Lunch", "<a@x>");
        ReflectionTestUtils.setField(t, "id", 7L);
        return t;
    }
}
