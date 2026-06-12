package com.emailmessenger.billing;

public record TrialConversionNudge(
        String planLabel,
        String planParam,
        long daysLeft,
        String monthlyPrice,
        String annualMonthlyEquivalent,
        String annualCashAmount,
        String dismissKey) {

    /**
     * Final-3-days window — the nudge surfaces an extra annual CTA
     * ("Save 2 months by switching to annual today") so a trial-end user
     * converts at the higher-ARPU SKU instead of the default monthly one.
     */
    public boolean inAnnualUpsellWindow() {
        return daysLeft <= 3;
    }
}
