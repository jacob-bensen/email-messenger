package com.emailmessenger.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LandingVideoTest {

    @Test
    void returnsNullWhenProviderUnset() {
        LandingProperties.Video v = new LandingProperties.Video();
        v.setId("dQw4w9WgXcQ");
        assertThat(LandingVideo.from(v)).isNull();
    }

    @Test
    void returnsNullWhenIdUnset() {
        LandingProperties.Video v = new LandingProperties.Video();
        v.setProvider("youtube");
        assertThat(LandingVideo.from(v)).isNull();
    }

    @Test
    void returnsNullForUnknownProvider() {
        LandingProperties.Video v = new LandingProperties.Video();
        v.setProvider("vimeo");
        v.setId("123456789");
        assertThat(LandingVideo.from(v)).isNull();
    }

    @Test
    void buildsYouTubeEmbedAgainstPrivacyEnhancedHost() {
        LandingProperties.Video v = new LandingProperties.Video();
        v.setProvider("YouTube"); // case- and whitespace-insensitive setter
        v.setId("dQw4w9WgXcQ");
        v.setTitle("MailIM in 60s");

        LandingVideo built = LandingVideo.from(v);

        assertThat(built).isNotNull();
        assertThat(built.provider()).isEqualTo("youtube");
        assertThat(built.embedUrl())
                .startsWith("https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ")
                .contains("rel=0").contains("modestbranding=1").contains("autoplay=1");
        assertThat(built.title()).isEqualTo("MailIM in 60s");
    }

    @Test
    void buildsLoomEmbedAndPropagatesPoster() {
        LandingProperties.Video v = new LandingProperties.Video();
        v.setProvider("loom");
        v.setId("abc123def456");
        v.setPosterUrl("https://cdn.example/poster.png");

        LandingVideo built = LandingVideo.from(v);

        assertThat(built).isNotNull();
        assertThat(built.embedUrl()).isEqualTo("https://www.loom.com/embed/abc123def456?autoplay=1");
        assertThat(built.posterUrl()).isEqualTo("https://cdn.example/poster.png");
        // The empty/blank title setter falls back to a sensible default.
        assertThat(built.title()).isEqualTo("MailIM demo");
    }

    @Test
    void rejectsIdsThatCouldBreakOutOfTheUrlAttribute() {
        // An operator-misconfigured id with quotes/spaces/control chars
        // must not be substituted into the embed URL — the regex guard
        // bounces it back to null so the template falls back to the mock.
        LandingProperties.Video v = new LandingProperties.Video();
        v.setProvider("youtube");
        v.setId("abc\" onload=alert(1) x=\"");
        assertThat(LandingVideo.from(v)).isNull();

        v.setId("abc/../malicious");
        assertThat(LandingVideo.from(v)).isNull();

        v.setId("a"); // below the 4-char floor
        assertThat(LandingVideo.from(v)).isNull();
    }

    @Test
    void titleSetterTrimsAndDefaultsBlank() {
        LandingProperties.Video v = new LandingProperties.Video();
        v.setTitle("   ");
        assertThat(v.getTitle()).isEqualTo("MailIM demo");
        v.setTitle("  Custom Demo  ");
        assertThat(v.getTitle()).isEqualTo("Custom Demo");
    }
}
