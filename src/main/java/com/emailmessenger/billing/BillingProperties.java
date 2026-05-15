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
    private String successUrl = "";
    private String cancelUrl = "";
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

    public String getSuccessUrl() { return successUrl; }
    public void setSuccessUrl(String v) { this.successUrl = v; }

    public String getCancelUrl() { return cancelUrl; }
    public void setCancelUrl(String v) { this.cancelUrl = v; }

    public int getTrialDays() { return trialDays; }
    public void setTrialDays(int v) { this.trialDays = v; }

    public Map<Plan, String> priceIds() {
        EnumMap<Plan, String> map = new EnumMap<>(Plan.class);
        map.put(Plan.PERSONAL, personalPriceId);
        map.put(Plan.TEAM, teamPriceId);
        map.put(Plan.ENTERPRISE, enterprisePriceId);
        return map;
    }
}
