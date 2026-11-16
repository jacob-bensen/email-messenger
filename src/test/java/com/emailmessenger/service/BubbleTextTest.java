package com.emailmessenger.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BubbleTextTest {

    @Test
    void keepsLineStructureOfParagraphsBreaksAndLists() {
        String html = "<p>Hi <strong>Ada</strong>,</p><p>line one<br>line two</p><ul><li>a</li><li>b</li></ul>";
        assertThat(BubbleText.of(html)).isEqualTo("Hi Ada,\nline one\nline two\na\nb");
    }

    @Test
    void collapsesWhitespaceAndBlankLinesAndDecodesEntities() {
        assertThat(BubbleText.of("  <p>  a   &amp;\n b </p><br><br><br><br><p>c</p> "))
                .isEqualTo("a & b\n\nc");
    }

    @Test
    void emptyInputIsEmpty() {
        assertThat(BubbleText.of(null)).isEmpty();
        assertThat(BubbleText.of("  ")).isEmpty();
        assertThat(BubbleText.of("<p></p>")).isEmpty();
    }
}
