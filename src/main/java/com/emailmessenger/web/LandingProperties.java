package com.emailmessenger.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Landing-page demo video configuration. When {@link Video#getProvider()}
 * and {@link Video#getId()} are both set, the hero swaps the static
 * chat-bubble mock for a click-to-play embed; otherwise the mock stays
 * in place so a fresh deploy without a video URL still renders cleanly.
 * Master overrides via env at deploy time:
 * {@code MARKETING_LANDING_VIDEO_PROVIDER=youtube},
 * {@code MARKETING_LANDING_VIDEO_ID=dQw4w9WgXcQ}, etc.
 */
@ConfigurationProperties("marketing.landing")
public class LandingProperties {

    private Video video = new Video();

    public Video getVideo() {
        return video;
    }

    public void setVideo(Video video) {
        this.video = video == null ? new Video() : video;
    }

    public static class Video {

        /** {@code youtube} or {@code loom}. Anything else disables the embed. */
        private String provider = "";

        /** Provider-specific video id (YouTube watch-id or Loom share-id). */
        private String id = "";

        /** Optional poster image URL shown until the visitor clicks play. */
        private String posterUrl = "";

        /** Accessible title for the iframe + the visible play-button label. */
        private String title = "ConexusMail demo";

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider == null ? "" : provider.trim().toLowerCase();
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id == null ? "" : id.trim();
        }

        public String getPosterUrl() {
            return posterUrl;
        }

        public void setPosterUrl(String posterUrl) {
            this.posterUrl = posterUrl == null ? "" : posterUrl.trim();
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title == null || title.isBlank() ? "ConexusMail demo" : title.trim();
        }
    }
}
