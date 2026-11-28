package com.emailmessenger.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SignatureCardsTest {

    @Test
    void blankSignatureHasNoCard() {
        assertThat(SignatureCards.parse("", "Ada Lovelace", "ada@acme.com")).isNull();
        assertThat(SignatureCards.parse(null, "Ada Lovelace", "ada@acme.com")).isNull();
    }

    @Test
    void readsTitleCompanyPhoneAndSiteFromLines() {
        ContactCard card = SignatureCards.parse(
                "<p>Ada Lovelace<br>Head of Engineering<br>Acme Corp<br>"
                        + "M: +1 (555) 123-4567<br>www.acme.com</p>",
                "Ada Lovelace", "ada@acme.com");

        assertThat(card.text()).isEqualTo(
                "Ada Lovelace\nHead of Engineering\nAcme Corp\nM: +1 (555) 123-4567\nwww.acme.com");
        assertThat(card.title()).isEqualTo("Head of Engineering");
        assertThat(card.company()).isEqualTo("Acme Corp");
        assertThat(card.phones()).containsExactly(new ContactCard.Phone("+1 (555) 123-4567", "+15551234567"));
        assertThat(card.websites()).containsExactly("https://www.acme.com");
        assertThat(card.hasFields()).isTrue();
    }

    @Test
    void splitsSeparatedSegmentsAndSkipsTheFirstName() {
        ContactCard card = SignatureCards.parse("<div>Ada · Acme · 555-1000</div>",
                "Ada Lovelace", "ada@acme.com");

        assertThat(card.title()).isEqualTo("Acme");
        assertThat(card.company()).isNull();
        assertThat(card.phones()).extracting(ContactCard.Phone::tel).containsExactly("5551000");
    }

    @Test
    void splitsTitleAtCompany() {
        ContactCard card = SignatureCards.parse("<p>Ada Lovelace | CTO at Acme Corp</p>",
                "Ada Lovelace", null);

        assertThat(card.title()).isEqualTo("CTO");
        assertThat(card.company()).isEqualTo("Acme Corp");
    }

    @Test
    void takesTelAndHttpLinksAndDedupesThem() {
        ContactCard card = SignatureCards.parse(
                "<p><a href=\"tel:+15551234567\">call me</a> +1 555 123 4567<br>"
                        + "<a href=\"https://acme.com/\">Acme</a> https://ACME.com "
                        + "<a href=\"mailto:ada@acme.com\">ada@acme.com</a></p>",
                "Ada Lovelace", "ada@acme.com");

        assertThat(card.phones()).containsExactly(new ContactCard.Phone("call me", "+15551234567"));
        assertThat(card.websites()).containsExactly("https://acme.com/");
    }

    @Test
    void ignoresSignOffsDatesAndShortNumbers() {
        ContactCard card = SignatureCards.parse(
                "<p>Best,<br>Ada<br>Office hours: Mon<br>Since 2026-11-28, ext 1234</p><p>Sent from my iPhone</p>",
                "Ada Lovelace", "ada@acme.com");

        assertThat(card.phones()).isEmpty();
        assertThat(card.websites()).isEmpty();
        assertThat(card.title()).isEqualTo("Office hours: Mon");
        assertThat(card.company()).isNull();
    }

    @Test
    void aSignatureWithNothingToReadStillHasItsText() {
        ContactCard card = SignatureCards.parse("<p>Sent from my iPhone</p>", "Ada", "ada@acme.com");

        assertThat(card.text()).isEqualTo("Sent from my iPhone");
        assertThat(card.hasFields()).isFalse();
    }

    @Test
    void capsPhonesAndWebsitesAtFive() {
        StringBuilder html = new StringBuilder("<p>");
        for (int i = 0; i < 7; i++) {
            html.append("555-100").append(i).append("<br>https://site").append(i).append(".com<br>");
        }
        ContactCard card = SignatureCards.parse(html.append("</p>").toString(), null, null);

        assertThat(card.phones()).hasSize(SignatureCards.MAX_ITEMS);
        assertThat(card.websites()).hasSize(SignatureCards.MAX_ITEMS)
                .startsWith("https://site0.com");
        assertThat(card.title()).isNull();
        assertThat(List.of(card.phones().get(0).display())).containsExactly("555-1000");
    }
}
