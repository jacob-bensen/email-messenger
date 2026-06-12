package com.emailmessenger.billing;

import com.emailmessenger.domain.Plan;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.EnumMap;
import java.util.Map;

@ConfigurationProperties("billing.stripe")
public class BillingProperties {

    private String secretKey = "";
    private String webhookSecret = "";
    private String personalPriceId = "";
    private String teamPriceId = "";
    private String enterprisePriceId = "";
    private String personalAnnualPriceId = "";
    private String teamAnnualPriceId = "";
    private String enterpriseAnnualPriceId = "";
    private String successUrl = "";
    private String cancelUrl = "";
    private String portalReturnUrl = "";
    private int trialDays = 14;

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String v) { this.secretKey = v; }

    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String v) { this.webhookSecret = v; }

    public String getPersonalPriceId() { return personalPriceId; }
    public void setPersonalPriceId(String v) { this.personalPriceId = v; }

    public String getTeamPriceId() { return teamPriceId; }
    public void setTeamPriceId(String v) { this.teamPriceId = v; }

    public String getEnterprisePriceId() { return enterprisePriceId; }
    public void setEnterprisePriceId(String v) { this.enterprisePriceId = v; }

    public String getPersonalAnnualPriceId() { return personalAnnualPriceId; }
    public void setPersonalAnnualPriceId(String v) { this.personalAnnualPriceId = v; }

    public String getTeamAnnualPriceId() { return teamAnnualPriceId; }
    public void setTeamAnnualPriceId(String v) { this.teamAnnualPriceId = v; }

    public String getEnterpriseAnnualPriceId() { return enterpriseAnnualPriceId; }
    public void setEnterpriseAnnualPriceId(String v) { this.enterpriseAnnualPriceId = v; }

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
        if (period == BillingPeriod.ANNUAL) {
            map.put(Plan.PERSONAL, personalAnnualPriceId);
            map.put(Plan.TEAM, teamAnnualPriceId);
            map.put(Plan.ENTERPRISE, enterpriseAnnualPriceId);
        } else {
            map.put(Plan.PERSONAL, personalPriceId);
            map.put(Plan.TEAM, teamPriceId);
            map.put(Plan.ENTERPRISE, enterprisePriceId);
        }
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
        if (priceId.equals(personalAnnualPriceId)
                || priceId.equals(teamAnnualPriceId)
                || priceId.equals(enterpriseAnnualPriceId)) {
            return BillingPeriod.ANNUAL;
        }
        if (priceId.equals(personalPriceId)
                || priceId.equals(teamPriceId)
                || priceId.equals(enterprisePriceId)) {
            return BillingPeriod.MONTHLY;
        }
        return null;
    }
}
