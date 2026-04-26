package com.emailmessenger.service;

import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.Message;
import com.emailmessenger.domain.Participant;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationServiceTest {

    private final ConversationService svc = new ConversationService(new IMTransformService());

    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2025, 1, 1, 10, 0);

    private EmailThread thread(String subject) {
        return new EmailThread(subject, "<root@test>");
    }

    private Message message(EmailThread t, Participant sender, String bodyPlain, int minuteOffset) {
        return new Message(t, sender, "Subject", bodyPlain, null, BASE_TIME.plusMinutes(minuteOffset));
    }

    // --- bubble run grouping ---

    @Test
    void emptyThreadProducesNoRuns() {
        Conversation conv = svc.buildConversation(thread("Empty"));
        assertThat(conv.runs()).isEmpty();
    }

    @Test
    void singleMessageProducesOneBubbleRun() {
        EmailThread t = thread("Test");
        Participant alice = new Participant("alice@test.com", "Alice");
        t.addMessage(message(t, alice, "Hello!", 0));

        Conversation conv = svc.buildConversation(t);

        assertThat(conv.runs()).hasSize(1);
        assertThat(conv.runs().get(0).sender()).isEqualTo(alice);
        assertThat(conv.runs().get(0).messages()).hasSize(1);
    }

    @Test
    void consecutiveSameSenderGroupedInOneBubbleRun() {
        EmailThread t = thread("Test");
        Participant alice = new Participant("alice@test.com", "Alice");
        t.addMessage(message(t, alice, "Message 1", 0));
        t.addMessage(message(t, alice, "Message 2", 1));
        t.addMessage(message(t, alice, "Message 3", 2));

        Conversation conv = svc.buildConversation(t);

        assertThat(conv.runs()).hasSize(1);
        assertThat(conv.runs().get(0).messages()).hasSize(3);
    }

    @Test
    void differentSendersProduceMultipleBubbleRuns() {
        EmailThread t = thread("Test");
        Participant alice = new Participant("alice@test.com", "Alice");
        Participant bob = new Participant("bob@test.com", "Bob");
        t.addMessage(message(t, alice, "Hi Bob", 0));
        t.addMessage(message(t, bob, "Hey Alice", 1));
        t.addMessage(message(t, alice, "How are you?", 2));

        Conversation conv = svc.buildConversation(t);

        assertThat(conv.runs()).hasSize(3);
        assertThat(conv.runs().get(0).sender().getEmail()).isEqualTo("alice@test.com");
        assertThat(conv.runs().get(1).sender().getEmail()).isEqualTo("bob@test.com");
        assertThat(conv.runs().get(2).sender().getEmail()).isEqualTo("alice@test.com");
    }

    @Test
    void mixedGroupsProduceCorrectRunCounts() {
        EmailThread t = thread("Test");
        Participant alice = new Participant("alice@test.com", "Alice");
        Participant bob = new Participant("bob@test.com", "Bob");
        // alice x2, bob x3, alice x1 → 3 runs
        t.addMessage(message(t, alice, "A1", 0));
        t.addMessage(message(t, alice, "A2", 1));
        t.addMessage(message(t, bob, "B1", 2));
        t.addMessage(message(t, bob, "B2", 3));
        t.addMessage(message(t, bob, "B3", 4));
        t.addMessage(message(t, alice, "A3", 5));

        Conversation conv = svc.buildConversation(t);

        assertThat(conv.runs()).hasSize(3);
        assertThat(conv.runs().get(0).messages()).hasSize(2);
        assertThat(conv.runs().get(1).messages()).hasSize(3);
        assertThat(conv.runs().get(2).messages()).hasSize(1);
    }

    // --- body HTML transform ---

    @Test
    void plainBodyIsTransformedToHtml() {
        EmailThread t = thread("Test");
        Participant alice = new Participant("alice@test.com", "Alice");
        t.addMessage(message(t, alice, "Hello **world**!", 0));

        Conversation conv = svc.buildConversation(t);

        String bodyHtml = conv.runs().get(0).messages().get(0).bodyHtml();
        assertThat(bodyHtml).contains("<strong>world</strong>");
    }

    @Test
    void htmlBodyPassesThroughWithoutTransform() {
        EmailThread t = thread("Test");
        Participant alice = new Participant("alice@test.com", "Alice");
        Message msg = new Message(t, alice, "Subject", "plain text",
            "<p>Already <b>HTML</b></p>", BASE_TIME);
        t.addMessage(msg);

        Conversation conv = svc.buildConversation(t);

        String bodyHtml = conv.runs().get(0).messages().get(0).bodyHtml();
        assertThat(bodyHtml).isEqualTo("<p>Already <b>HTML</b></p>");
    }

    @Test
    void quotedRepliesStrippedBeforeRendering() {
        EmailThread t = thread("Test");
        Participant alice = new Participant("alice@test.com", "Alice");
        String body = "My reply.\n\nOn Mon, Jun 5 wrote:\n> Old content";
        t.addMessage(message(t, alice, body, 0));

        Conversation conv = svc.buildConversation(t);

        String bodyHtml = conv.runs().get(0).messages().get(0).bodyHtml();
        assertThat(bodyHtml).doesNotContain("Old content");
        assertThat(bodyHtml).contains("My reply");
    }
}
