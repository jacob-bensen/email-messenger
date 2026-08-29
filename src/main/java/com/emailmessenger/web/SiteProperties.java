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
    private String baseUrl = "https://conexusmail.com";

    /**
     * User-facing support address surfaced in the marketing footers, the
     * account settings help line, and the error page. Overridable via
     * {@code MARKETING_SUPPORT_EMAIL}; the setter trims whitespace and
     * strips a leading {@code mailto:} so an operator that pastes the
     * whole href doesn't end up with {@code mailto:mailto:...} on the
     * page. Blank / null falls back to the default.
     */
    private String supportEmail = "support@conexusmail.com";

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

    public String getSupportEmail() {
        return supportEmail;
    }

    public void setSupportEmail(String supportEmail) {
        if (supportEmail == null) {
            this.supportEmail = "support@conexusmail.com";
            return;
        }
        String trimmed = supportEmail.trim();
        if (trimmed.regionMatches(true, 0, "mailto:", 0, 7)) {
            trimmed = trimmed.substring(7).trim();
        }
        if (trimmed.isEmpty()) {
            this.supportEmail = "support@conexusmail.com";
            return;
        }
        this.supportEmail = trimmed;
    }
}
