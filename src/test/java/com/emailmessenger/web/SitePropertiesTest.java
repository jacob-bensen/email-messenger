package com.emailmessenger.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SitePropertiesTest {

    @Test
    void supportEmailDefaultsToConexusmailAddress() {
        assertThat(new SiteProperties().getSupportEmail())
                .isEqualTo("support@conexusmail.com");
    }

    @Test
    void supportEmailSetterTrimsWhitespace() {
        SiteProperties p = new SiteProperties();
        p.setSupportEmail("  hello@example.com \n");
        assertThat(p.getSupportEmail()).isEqualTo("hello@example.com");
    }

    @Test
    void supportEmailSetterStripsMailtoPrefixSoOperatorCanPasteHref() {
        SiteProperties p = new SiteProperties();
        p.setSupportEmail("mailto:help@example.com");
        assertThat(p.getSupportEmail()).isEqualTo("help@example.com");
        // Case-insensitive prefix + surrounding whitespace both handled.
        p.setSupportEmail("  MailTo:help@example.com  ");
        assertThat(p.getSupportEmail()).isEqualTo("help@example.com");
    }

    @Test
    void supportEmailFallsBackToDefaultOnNullOrBlank() {
        SiteProperties p = new SiteProperties();
        p.setSupportEmail("real@example.com");
        p.setSupportEmail(null);
        assertThat(p.getSupportEmail()).isEqualTo("support@conexusmail.com");
        p.setSupportEmail("real@example.com");
        p.setSupportEmail("   ");
        assertThat(p.getSupportEmail()).isEqualTo("support@conexusmail.com");
        p.setSupportEmail("real@example.com");
        p.setSupportEmail("mailto:");
        assertThat(p.getSupportEmail()).isEqualTo("support@conexusmail.com");
    }

    @Test
    void baseUrlSetterStripsTrailingSlashesAndToleratesNull() {
        SiteProperties p = new SiteProperties();
        p.setBaseUrl("https://example.com/");
        assertThat(p.getBaseUrl()).isEqualTo("https://example.com");
        p.setBaseUrl("https://example.com///");
        assertThat(p.getBaseUrl()).isEqualTo("https://example.com");
        p.setBaseUrl(null);
        assertThat(p.getBaseUrl()).isEmpty();
    }
}
