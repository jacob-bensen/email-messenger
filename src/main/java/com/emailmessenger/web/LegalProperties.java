package com.emailmessenger.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Pointers to the HTML body content rendered inside the legal-page
 * templates. Defaults to classpath boilerplate that ships with the app;
 * Master can override any of the three at deploy time via env (e.g.
 * {@code MARKETING_LEGAL_PRIVACY=file:/etc/conexusmail/privacy.html}) to drop
 * in Termly/Iubenda output without a redeploy or template change.
 */
@ConfigurationProperties("marketing.legal")
public class LegalProperties {

    private String privacy = "classpath:legal/privacy.html";
    private String terms = "classpath:legal/terms.html";
    private String refund = "classpath:legal/refund.html";

    public String getPrivacy() {
        return privacy;
    }

    public void setPrivacy(String privacy) {
        this.privacy = privacy;
    }

    public String getTerms() {
        return terms;
    }

    public void setTerms(String terms) {
        this.terms = terms;
    }

    public String getRefund() {
        return refund;
    }

    public void setRefund(String refund) {
        this.refund = refund;
    }
}
