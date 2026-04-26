package com.emailmessenger.service;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class IMTransformServiceTest {

    private final IMTransformService svc = new IMTransformService();

    // --- stripQuotes ---

    @Test
    void stripQuotes_nullBodyReturnsEmpty() {
        assertThat(svc.stripQuotes(null)).isEmpty();
    }

    @Test
    void stripQuotes_preservesContentWithNoQuote() {
        String body = "Hello there!\n\nHow are you?";
        assertThat(svc.stripQuotes(body)).isEqualTo(body);
    }

    @Test
    void stripQuotes_removesGreaterThanLines() {
        String body = "My reply.\n> Original line\n> Another quoted line";
        assertThat(svc.stripQuotes(body)).isEqualTo("My reply.");
    }

    @Test
    void stripQuotes_removesGmailAttributionAndEverythingAfter() {
        String body = "My reply.\n\nOn Mon, Jun 5, 2023 at 3:14 PM, Alice <a@x.com> wrote:\n> Quoted text here";
        String result = svc.stripQuotes(body);
        assertThat(result).isEqualTo("My reply.");
        assertThat(result).doesNotContain("Alice");
        assertThat(result).doesNotContain("Quoted text");
    }

    @Test
    void stripQuotes_removesWrappedAttributionSpanningTwoLines() {
        String body = "My reply.\n\nOn Mon, Jun 5, 2023 at 3:14 PM, Alice\nwrote:\n> Old message";
        String result = svc.stripQuotes(body);
        assertThat(result).isEqualTo("My reply.");
    }

    @Test
    void stripQuotes_removesOutlookOriginalMessageDivider() {
        String body = "My reply.\n\n-----Original Message-----\nFrom: Bob\nSubject: Old thing\n\nOld content";
        String result = svc.stripQuotes(body);
        assertThat(result).isEqualTo("My reply.");
        assertThat(result).doesNotContain("Bob");
        assertThat(result).doesNotContain("Old content");
    }

    @Test
    void stripQuotes_collapsesExcessiveBlankLines() {
        String body = "Line one.\n\n\n\n\nLine two.";
        assertThat(svc.stripQuotes(body)).isEqualTo("Line one.\n\nLine two.");
    }

    // --- renderMarkdown ---

    @Test
    void renderMarkdown_nullOrBlankReturnsEmpty() {
        assertThat(svc.renderMarkdown(null)).isEmpty();
        assertThat(svc.renderMarkdown("   ")).isEmpty();
    }

    @Test
    void renderMarkdown_bold() {
        String result = svc.renderMarkdown("This is **bold** text.");
        assertThat(result).contains("<strong>bold</strong>");
    }

    @Test
    void renderMarkdown_italic() {
        String result = svc.renderMarkdown("This is *italic* text.");
        assertThat(result).contains("<em>italic</em>");
    }

    @Test
    void renderMarkdown_inlineCode() {
        String result = svc.renderMarkdown("Use `git status` to check.");
        assertThat(result).contains("<code>git status</code>");
    }

    @Test
    void renderMarkdown_urlBecomesAnchor() {
        String result = svc.renderMarkdown("See https://example.com for details.");
        assertThat(result).contains("<a href=\"https://example.com\">https://example.com</a>");
    }

    @Test
    void renderMarkdown_blankLineProducesParagraphs() {
        String result = svc.renderMarkdown("Para one.\n\nPara two.");
        assertThat(result).contains("<p>Para one.</p>");
        assertThat(result).contains("<p>Para two.</p>");
    }

    @Test
    void renderMarkdown_escapesHtmlSpecialChars() {
        String result = svc.renderMarkdown("5 < 10 & \"quotes\"");
        assertThat(result).contains("&lt;");
        assertThat(result).contains("&amp;");
        assertThat(result).contains("&quot;");
        assertThat(result).doesNotContain("<10");
    }
}
