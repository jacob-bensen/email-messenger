package com.emailmessenger.billing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BillingPeriodTest {

    @Test
    void parseAnnualValue() {
        assertThat(BillingPeriod.parse("annual")).isEqualTo(BillingPeriod.ANNUAL);
    }

    @Test
    void parseMonthlyValue() {
        assertThat(BillingPeriod.parse("monthly")).isEqualTo(BillingPeriod.MONTHLY);
    }

    @Test
    void parseUppercaseAnnualResolvesToAnnual() {
        assertThat(BillingPeriod.parse("ANNUAL")).isEqualTo(BillingPeriod.ANNUAL);
    }

    @Test
    void parseTrimsWhitespaceAroundValue() {
        assertThat(BillingPeriod.parse("  annual  ")).isEqualTo(BillingPeriod.ANNUAL);
    }

    @Test
    void parseNullDefaultsToMonthly() {
        assertThat(BillingPeriod.parse(null)).isEqualTo(BillingPeriod.MONTHLY);
    }

    @Test
    void parseBlankDefaultsToMonthly() {
        assertThat(BillingPeriod.parse("")).isEqualTo(BillingPeriod.MONTHLY);
        assertThat(BillingPeriod.parse("   ")).isEqualTo(BillingPeriod.MONTHLY);
    }

    @Test
    void parseUnknownValueDefaultsToMonthlyInsteadOfThrowing() {
        // A tampered or future-cadence query string must never throw — the
        // controller hands the result straight to BillingService and a 500
        // mid-checkout strands the user worse than a silent monthly fallback.
        assertThat(BillingPeriod.parse("quarterly")).isEqualTo(BillingPeriod.MONTHLY);
        assertThat(BillingPeriod.parse("'; DROP TABLE users--")).isEqualTo(BillingPeriod.MONTHLY);
    }

    @Test
    void paramValueIsLowercase() {
        assertThat(BillingPeriod.MONTHLY.paramValue()).isEqualTo("monthly");
        assertThat(BillingPeriod.ANNUAL.paramValue()).isEqualTo("annual");
    }
}
