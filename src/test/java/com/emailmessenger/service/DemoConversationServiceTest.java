package com.emailmessenger.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class DemoConversationServiceTest {

    private final DemoConversationService demo = new DemoConversationService(
            new ConversationService(new IMTransformService()));

    @Test
    void demoConversationHasCuratedSubjectAndMultipleParticipants() {
        Conversation conv = demo.buildDemo(LocalDate.of(2026, 6, 4));

        assertThat(conv.thread().getSubject()).isEqualTo("Launch checklist — Tuesday demo");
        Set<String> senders = conv.runs().stream()
                .map(r -> r.sender().getDisplayName())
                .collect(Collectors.toSet());
        assertThat(senders).containsExactlyInAnyOrder("Alex Lee", "Sam Patel", "Maya Chen");
    }

    @Test
    void consecutiveSameSenderMessagesGroupIntoOneBubbleRun() {
        Conversation conv = demo.buildDemo(LocalDate.of(2026, 6, 4));

        // Alex sends two messages back-to-back at the start — must collapse into one run.
        BubbleRun first = conv.runs().get(0);
        assertThat(first.sender().getDisplayName()).isEqualTo("Alex Lee");
        assertThat(first.messages()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void demoSpansYesterdayAndToday() {
        LocalDate today = LocalDate.of(2026, 6, 4);
        Conversation conv = demo.buildDemo(today);

        List<LocalDate> dates = conv.runs().stream()
                .map(BubbleRun::date)
                .distinct()
                .toList();

        assertThat(dates).contains(today, today.minusDays(1));
    }

    @Test
    void quotedReplyBlockIsStrippedFromRenderedBody() {
        Conversation conv = demo.buildDemo(LocalDate.of(2026, 6, 4));

        String allBodies = conv.runs().stream()
                .flatMap(r -> r.messages().stream())
                .map(BubbleMessage::bodyHtml)
                .collect(Collectors.joining("\n"));

        // "> Two things blocking launch:" appears verbatim only inside a quoted-reply block —
        // if MailIM's quote stripper works, that text should not survive into the rendered HTML.
        assertThat(allBodies).doesNotContain("&gt; Two things blocking launch");
        assertThat(allBodies).doesNotContain("On Mon, Jun 3, Alex Lee wrote:");
    }

    @Test
    void markdownBoldRendersAsStrongTagsInBubbles() {
        Conversation conv = demo.buildDemo(LocalDate.of(2026, 6, 4));

        String allBodies = conv.runs().stream()
                .flatMap(r -> r.messages().stream())
                .map(BubbleMessage::bodyHtml)
                .collect(Collectors.joining("\n"));

        // Maya's "Your inbox, as a chat." reply uses **…** — the IM transform must render it.
        assertThat(allBodies).contains("<strong>Your inbox, as a chat.</strong>");
    }

    @Test
    void messageCountReflectsAllAddedMessages() {
        Conversation conv = demo.buildDemo(LocalDate.of(2026, 6, 4));

        int bubbleCount = conv.runs().stream().mapToInt(r -> r.messages().size()).sum();
        assertThat(conv.thread().getMessageCount()).isEqualTo(bubbleCount);
        assertThat(conv.thread().getMessageCount()).isGreaterThanOrEqualTo(7);
    }

    @Test
    void buildDemoUsesCurrentDateByDefault() {
        Conversation conv = demo.buildDemo();

        List<LocalDate> dates = conv.runs().stream()
                .map(BubbleRun::date)
                .distinct()
                .toList();

        // No demo message is dated in the future.
        assertThat(dates).allSatisfy(d -> assertThat(d).isBeforeOrEqualTo(LocalDate.now()));
        assertThat(dates).contains(LocalDate.now());
    }
}
