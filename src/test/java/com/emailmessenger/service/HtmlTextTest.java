package com.emailmessenger.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlTextTest {

    @Test
    void paragraphsAreSeparatedByABlankLine() {
        assertThat(HtmlText.toPlainText("<p>Hi Ada,</p><p>Lunch at <b>noon</b>?<br>Bob</p>"))
                .isEqualTo("Hi Ada,\n\nLunch at noon?\nBob");
    }

    @Test
    void collapsesBlankLinesAndEntities() {
        assertThat(HtmlText.toPlainText("<div>a</div><br><br><br><div>b&nbsp;&amp;&nbsp;c</div>"))
                .isEqualTo("a\n\nb & c");
    }

    @Test
    void listsAndPreformattedText() {
        assertThat(HtmlText.toPlainText("<ul><li>one</li><li>two</li></ul><pre>  x\n  y</pre>"))
                .isEqualTo("one\ntwo\n\nx\ny");
    }

    @Test
    void emptyInput() {
        assertThat(HtmlText.toPlainText(null)).isEmpty();
        assertThat(HtmlText.toPlainText("  ")).isEmpty();
        assertThat(HtmlText.toPlainText("<p> </p>")).isEmpty();
    }
}
