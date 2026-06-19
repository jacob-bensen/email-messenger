package com.emailmessenger.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailBodyCleanerTest {

    private final EmailBodyCleaner cleaner = new EmailBodyCleaner();

    @Test
    void stripsOutlookSignatureAndQuotedChainKeepingOnlyNewContent() {
        String html =
                "<div>Thanks Jacob! I will send your loan over to my processor.</div>"
              + "<div>&nbsp;</div>"
              + "<div>Looking forward to working with you.</div>"
              + "<div id=\"Signature\"><p>Jonathan Devin</p><p>NMLS #1473968</p>"
              + "<p>Phone: 267-245-7946</p></div>"
              + "<div id=\"appendonsend\"></div>"
              + "<div><b>From:</b> Jacob Bensen<br><b>Sent:</b> Thursday<br>"
              + "<b>To:</b> Jonathan Devin<br><b>Subject:</b> Re: Steps</div>"
              + "<div><p>Hi Jonathan,</p><p>I think I have everything here.</p></div>";

        String out = cleaner.clean(html);

        assertThat(out).contains("Thanks Jacob");
        assertThat(out).contains("Looking forward to working with you");
        // Signature gone.
        assertThat(out).doesNotContain("Jonathan Devin");
        assertThat(out).doesNotContain("NMLS");
        // Quoted chain gone.
        assertThat(out).doesNotContain("From:");
        assertThat(out).doesNotContain("I think I have everything");
    }

    @Test
    void cutsInlineFromHeaderEvenWithoutOutlookIds() {
        String html = "<p>My reply here.</p>"
                + "<div>From: Someone<br>Sent: today<br>Subject: Re: hi</div>"
                + "<div>the old quoted message</div>";

        String out = cleaner.clean(html);

        assertThat(out).contains("My reply here");
        assertThat(out).doesNotContain("From:");
        assertThat(out).doesNotContain("the old quoted message");
    }

    @Test
    void removesBlockquoteAndOnWroteAttribution() {
        String html = "<p>Sounds good.</p>"
                + "<div>On Mon, Jun 1, Alice wrote:</div>"
                + "<blockquote>earlier content</blockquote>";

        String out = cleaner.clean(html);

        assertThat(out).contains("Sounds good");
        assertThat(out).doesNotContain("earlier content");
        assertThat(out).doesNotContain("wrote:");
    }

    @Test
    void dropsEmptyAndWhitespaceOnlyBlocks() {
        // ﻿ is the BOM/zero-width char that bloats real messages.
        String html = "<p>Hi</p><p>&nbsp;</p><p></p><div>﻿</div>";

        String out = cleaner.clean(html);

        assertThat(out).contains("Hi");
        assertThat(out).doesNotContain("<p></p>");
        assertThat(out).doesNotContain("﻿");
    }

    @Test
    void preservesOrdinaryFormattedContent() {
        String out = cleaner.clean("<p>Hello <b>world</b> and <a href=\"https://x.com\">link</a></p>");

        assertThat(out).contains("Hello");
        assertThat(out).contains("<b>world</b>");
        assertThat(out).contains("href=\"https://x.com\"");
    }

    @Test
    void blankInputReturnsEmpty() {
        assertThat(cleaner.clean(null)).isEmpty();
        assertThat(cleaner.clean("   ")).isEmpty();
    }
}
