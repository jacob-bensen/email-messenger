package com.emailmessenger.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DemoServiceTest {

    private final DemoService service = new DemoService();

    @Test
    void listThreadsReturnsTwoEntries() {
        assertThat(service.listThreads()).hasSize(2);
    }

    @Test
    void catalogEntriesHaveCorrectMessageCounts() {
        var catalog = service.listThreads();
        assertThat(catalog.get(0).messageCount()).isEqualTo(3);
        assertThat(catalog.get(1).messageCount()).isEqualTo(5);
    }

    @Test
    void catalogEntriesHaveNonBlankSubjects() {
        service.listThreads().forEach(e -> assertThat(e.subject()).isNotBlank());
    }

    @Test
    void demoConversation1HasThreeBubbleMessages() {
        Conversation conv = service.getConversation(1);
        assertThat(conv).isNotNull();
        int total = conv.runs().stream().mapToInt(r -> r.messages().size()).sum();
        assertThat(total).isEqualTo(3);
    }

    @Test
    void demoConversation2HasFiveBubbleMessages() {
        Conversation conv = service.getConversation(2);
        assertThat(conv).isNotNull();
        int total = conv.runs().stream().mapToInt(r -> r.messages().size()).sum();
        assertThat(total).isEqualTo(5);
    }

    @Test
    void demoConversationSubjectsMatchCatalog() {
        var catalog = service.listThreads();
        assertThat(service.getConversation(1).thread().getSubject())
            .isEqualTo(catalog.get(0).subject());
        assertThat(service.getConversation(2).thread().getSubject())
            .isEqualTo(catalog.get(1).subject());
    }

    @Test
    void demoConversationBodiesContainSafeHtml() {
        Conversation conv = service.getConversation(1);
        conv.runs().forEach(run ->
            run.messages().forEach(msg ->
                assertThat(msg.bodyHtml()).doesNotContain("<script>")
            )
        );
    }

    @Test
    void unknownDemoIdReturnsNull() {
        assertThat(service.getConversation(0)).isNull();
        assertThat(service.getConversation(99)).isNull();
    }

    @Test
    void demoConversationSentAtTimesAreChronological() {
        Conversation conv = service.getConversation(2);
        var allTimes = conv.runs().stream()
            .flatMap(r -> r.messages().stream())
            .map(BubbleMessage::sentAt)
            .toList();
        for (int i = 1; i < allTimes.size(); i++) {
            assertThat(allTimes.get(i)).isAfterOrEqualTo(allTimes.get(i - 1));
        }
    }
}
