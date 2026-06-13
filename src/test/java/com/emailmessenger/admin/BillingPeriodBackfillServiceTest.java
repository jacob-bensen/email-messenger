package com.emailmessenger.admin;

import com.emailmessenger.billing.BillingPeriod;
import com.emailmessenger.billing.BillingProperties;
import com.emailmessenger.billing.StripeSubscriptionGateway;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.Subscription;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingPeriodBackfillServiceTest {

    @Mock SubscriptionRepository subscriptions;
    @Mock StripeSubscriptionGateway gateway;

    private final BillingProperties properties = new BillingProperties();
    private BillingPeriodBackfillService service;

    @BeforeEach
    void setUp() {
        properties.setPersonalPriceId("price_personal_monthly");
        properties.setTeamPriceId("price_team_monthly");
        properties.setEnterprisePriceId("price_enterprise_monthly");
        properties.setPersonalAnnualPriceId("price_personal_annual");
        properties.setTeamAnnualPriceId("price_team_annual");
        properties.setEnterpriseAnnualPriceId("price_enterprise_annual");
        service = new BillingPeriodBackfillService(subscriptions, gateway, properties);
    }

    @Test
    void noCandidatesYieldsNoOpResult() {
        when(subscriptions.findByBillingPeriodIsNull()).thenReturn(List.of());

        BillingPeriodBackfillResult result = service.reconcile();
        assertThat(result.isNoOp()).isTrue();
        assertThat(result.scanned()).isZero();
        assertThat(result.updated()).isZero();
    }

    @Test
    void monthlyPriceFromStripeFillsInBillingPeriod() {
        Subscription sub = nullPeriodSub("payer@example.com", "sub_123");
        when(subscriptions.findByBillingPeriodIsNull()).thenReturn(List.of(sub));
        when(gateway.currentPriceId("sub_123"))
                .thenReturn(Optional.of("price_personal_monthly"));

        BillingPeriodBackfillResult result = service.reconcile();
        assertThat(result.scanned()).isEqualTo(1);
        assertThat(result.updated()).isEqualTo(1);
        assertThat(sub.getBillingPeriod()).isEqualTo(BillingPeriod.MONTHLY);
        assertThat(sub.getStripePriceId()).isEqualTo("price_personal_monthly");
    }

    @Test
    void annualPriceFromStripeFillsInBillingPeriod() {
        Subscription sub = nullPeriodSub("payer@example.com", "sub_456");
        when(subscriptions.findByBillingPeriodIsNull()).thenReturn(List.of(sub));
        when(gateway.currentPriceId("sub_456"))
                .thenReturn(Optional.of("price_team_annual"));

        BillingPeriodBackfillResult result = service.reconcile();
        assertThat(result.updated()).isEqualTo(1);
        assertThat(sub.getBillingPeriod()).isEqualTo(BillingPeriod.ANNUAL);
    }

    @Test
    void missingStripeSubscriptionIdIsSkippedNotUpdated() {
        Subscription sub = nullPeriodSub("incomplete@example.com", null);
        when(subscriptions.findByBillingPeriodIsNull()).thenReturn(List.of(sub));

        BillingPeriodBackfillResult result = service.reconcile();
        assertThat(result.scanned()).isEqualTo(1);
        assertThat(result.updated()).isZero();
        assertThat(result.missingStripeId()).isEqualTo(1);
        assertThat(sub.getBillingPeriod()).isNull();
    }

    @Test
    void priceIdNotMatchingConfiguredSkuIsLoggedAsUnmatchedAndDoesNotOverwrite() {
        Subscription sub = nullPeriodSub("promo@example.com", "sub_promo");
        when(subscriptions.findByBillingPeriodIsNull()).thenReturn(List.of(sub));
        when(gateway.currentPriceId("sub_promo"))
                .thenReturn(Optional.of("price_legacy_promo"));

        BillingPeriodBackfillResult result = service.reconcile();
        assertThat(result.unmatchedPriceId()).isEqualTo(1);
        assertThat(result.updated()).isZero();
        assertThat(sub.getBillingPeriod()).isNull();
    }

    @Test
    void stripeFailureCountsAsMissAndLeavesRowUntouched() {
        Subscription sub = nullPeriodSub("ghost@example.com", "sub_deleted");
        when(subscriptions.findByBillingPeriodIsNull()).thenReturn(List.of(sub));
        when(gateway.currentPriceId("sub_deleted")).thenReturn(Optional.empty());

        BillingPeriodBackfillResult result = service.reconcile();
        assertThat(result.stripeMisses()).isEqualTo(1);
        assertThat(result.updated()).isZero();
        assertThat(sub.getBillingPeriod()).isNull();
    }

    @Test
    void mixedBatchPartitionsCleanlyAcrossOutcomeBuckets() {
        Subscription happy = nullPeriodSub("a@example.com", "sub_happy");
        Subscription missing = nullPeriodSub("b@example.com", null);
        Subscription unmatched = nullPeriodSub("c@example.com", "sub_promo");
        Subscription miss = nullPeriodSub("d@example.com", "sub_gone");
        when(subscriptions.findByBillingPeriodIsNull())
                .thenReturn(List.of(happy, missing, unmatched, miss));
        when(gateway.currentPriceId("sub_happy"))
                .thenReturn(Optional.of("price_enterprise_annual"));
        when(gateway.currentPriceId("sub_promo"))
                .thenReturn(Optional.of("price_unknown"));
        when(gateway.currentPriceId("sub_gone")).thenReturn(Optional.empty());

        BillingPeriodBackfillResult result = service.reconcile();
        assertThat(result.scanned()).isEqualTo(4);
        assertThat(result.updated()).isEqualTo(1);
        assertThat(result.missingStripeId()).isEqualTo(1);
        assertThat(result.unmatchedPriceId()).isEqualTo(1);
        assertThat(result.stripeMisses()).isEqualTo(1);
        assertThat(happy.getBillingPeriod()).isEqualTo(BillingPeriod.ANNUAL);
    }

    @Test
    void secondRunAfterFirstCompletesIsANoOpBecauseCandidateSetEmpties() {
        // First run: one candidate, gateway returns a known price, row is updated.
        Subscription sub = nullPeriodSub("payer@example.com", "sub_99");
        when(subscriptions.findByBillingPeriodIsNull())
                .thenReturn(List.of(sub))
                .thenReturn(List.of());
        when(gateway.currentPriceId("sub_99"))
                .thenReturn(Optional.of("price_personal_monthly"));

        BillingPeriodBackfillResult first = service.reconcile();
        BillingPeriodBackfillResult second = service.reconcile();
        assertThat(first.updated()).isEqualTo(1);
        assertThat(second.isNoOp()).isTrue();
    }

    private static Subscription nullPeriodSub(String email, String stripeSubscriptionId) {
        User u = new User(email, "hash", null);
        Subscription s = new Subscription(u, "cus_" + email, "active");
        s.setPlan(Plan.PERSONAL);
        if (stripeSubscriptionId != null) {
            s.setStripeSubscriptionId(stripeSubscriptionId);
        }
        return s;
    }
}
