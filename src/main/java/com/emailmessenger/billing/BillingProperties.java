package com.emailmessenger.billing;

import com.emailmessenger.domain.Plan;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.EnumMap;
import java.util.Map;

@ConfigurationProperties("billing.stripe")
public class BillingProperties {

    private String secretKey = "";
    private String webhookSecret = "";
    private String proPriceId = "";
    private String proAnnualPriceId = "";
    private String successUrl = "";
    private String cancelUrl = "";
    private String portalReturnUrl = "";
    private int trialDays = 14;

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String v) { this.secretKey = v; }

    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String v) { this.webhookSecret = v; }

    public String getProPriceId() { return proPriceId; }
    public void setProPriceId(String v) { this.proPriceId = v; }

    public String getProAnnualPriceId() { return proAnnualPriceId; }
    public void setProAnnualPriceId(String v) { this.proAnnualPriceId = v; }

    public String getSuccessUrl() { return successUrl; }
    public void setSuccessUrl(String v) { this.successUrl = v; }

    public String getCancelUrl() { return cancelUrl; }
    public void setCancelUrl(String v) { this.cancelUrl = v; }

    public String getPortalReturnUrl() { return portalReturnUrl; }
    public void setPortalReturnUrl(String v) { this.portalReturnUrl = v; }

    public int getTrialDays() { return trialDays; }
    public void setTrialDays(int v) { this.trialDays = v; }

    public Map<Plan, String> priceIds(BillingPeriod period) {
        EnumMap<Plan, String> map = new EnumMap<>(Plan.class);
        // Pro is the only self-serve checkout plan; Business is sales-assisted
        // and never flows through Stripe Checkout from the app.
        map.put(Plan.PRO, period == BillingPeriod.ANNUAL ? proAnnualPriceId : proPriceId);
        return map;
    }

    /**
     * Reverse lookup: given a Stripe price ID (from a webhook payload or a
     * previously-completed checkout), report which billing cadence it
     * belongs to. Returns {@code null} when the price ID doesn't match any
     * configured SKU — leave the existing recorded period alone in that
     * case rather than guess.
     */
    public BillingPeriod periodFor(String priceId) {
        if (priceId == null || priceId.isEmpty()) {
            return null;
        }
        if (priceId.equals(proAnnualPriceId)) {
            return BillingPeriod.ANNUAL;
        }
        if (priceId.equals(proPriceId)) {
            return BillingPeriod.MONTHLY;
        }
        return null;
    }
}
