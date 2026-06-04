package com.emailmessenger.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("marketing")
public class SiteProperties {

    /**
     * Public base URL used for canonical links, OG/Twitter URLs, and
     * the sitemap. No trailing slash. Set via MARKETING_BASE_URL in
     * prod; defaulted to the production domain so dev renders something
     * sensible even without env-var overrides.
     */
    private String baseUrl = "https://mailaim.app";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        if (baseUrl == null) {
            this.baseUrl = "";
            return;
        }
        String trimmed = baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        this.baseUrl = trimmed;
    }
}
