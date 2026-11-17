package com.emailmessenger.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlTextTest {

    @Test
    void keepsLineAndParagraphBreaks() {
        assertThat(HtmlText.of("<p>Hi Ada,</p><p>Line one<br>line two</p>"))
                .isEqualTo("Hi Ada,\n\nLine one\nline two");
    }

    @Test
    void decodesEntitiesAndListItems() {
        assertThat(HtmlText.of("Fish &amp; chips&nbsp;now<ul><li>a</li><li>b</li></ul>"))
                .isEqualTo("Fish & chips now\n\na\n\nb");
    }

    @Test
    void blankIsEmpty() {
        assertThat(HtmlText.of(null)).isEmpty();
        assertThat(HtmlText.of("  ")).isEmpty();
        assertThat(HtmlText.of("<p> </p>")).isEmpty();
    }
}
