package com.emailmessenger.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiTextTest {

    @Test
    void nullAndBlankAreEmpty() {
        assertThat(ApiText.plain(null)).isEmpty();
        assertThat(ApiText.plain("  ")).isEmpty();
    }

    @Test
    void blocksAndBreaksBecomeNewlines() {
        assertThat(ApiText.plain("<p>Hi Ada,</p><p>Line one<br>line two</p>"))
                .isEqualTo("Hi Ada,\nLine one\nline two");
    }

    @Test
    void inlineMarkupIsFlattenedAndEntitiesDecoded() {
        assertThat(ApiText.plain("<p>Tom &amp; <strong>Jerry</strong> <a href=\"https://x.test\">link</a></p>"))
                .isEqualTo("Tom & Jerry link");
    }

    @Test
    void runsOfBlankLinesCollapse() {
        assertThat(ApiText.plain("<div>a</div><div><br></div><div><br></div><div>b</div>"))
                .isEqualTo("a\n\nb");
    }
}
