package com.emailmessenger.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlTextTest {

    @Test
    void blankOrNullIsEmpty() {
        assertThat(HtmlText.toPlainText(null)).isEmpty();
        assertThat(HtmlText.toPlainText("  ")).isEmpty();
    }

    @Test
    void paragraphsAndBreaksBecomeLines() {
        assertThat(HtmlText.toPlainText("<p>Hi Ada,</p><p>Pizza at noon?<br>Or 1pm</p>"))
                .isEqualTo("Hi Ada,\n\nPizza at noon?\nOr 1pm");
    }

    @Test
    void collapsesWhitespaceAndDecodesEntities() {
        assertThat(HtmlText.toPlainText("<div>  fish &amp;\n   chips&nbsp;&lt;3 </div><div></div><div></div>"))
                .isEqualTo("fish & chips <3");
    }

    @Test
    void listItemsAreSeparateLines() {
        assertThat(HtmlText.toPlainText("<ul><li>one</li><li><b>two</b> items</li></ul>"))
                .isEqualTo("one\n\ntwo items");
    }
}
