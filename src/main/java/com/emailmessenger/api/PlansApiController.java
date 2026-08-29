package com.emailmessenger.api;

import com.emailmessenger.billing.BillingPeriod;
import com.emailmessenger.billing.PlanLimits;
import com.emailmessenger.billing.PlanPricing;
import com.emailmessenger.domain.Plan;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * Public read-only pricing catalog for the mobile client. Sources every
 * figure from the same billing helpers the admin MRR calc uses so the
 * mobile app, the /pricing Thymeleaf page, and the operator dashboard
 * cannot drift.
 */
@RestController
@RequestMapping("/api/v1")
class PlansApiController {

    @GetMapping("/plans")
    PlansResponse list() {
        List<PlanResource> plans = Arrays.stream(Plan.values())
                .map(PlansApiController::resource)
                .toList();
        return new PlansResponse(plans);
    }

    private static PlanResource resource(Plan plan) {
        PlanLimits limits = PlanLimits.forPlan(plan);
        return new PlanResource(
                plan.name(),
                PlanPricing.monthlyCents(plan, BillingPeriod.MONTHLY),
                PlanPricing.monthlyCents(plan, BillingPeriod.ANNUAL),
                nullIfUnlimited(limits.mailboxes()),
                nullIfUnlimited(limits.threads()),
                nullIfUnlimited(limits.savedSearches()));
    }

    private static Long nullIfUnlimited(long value) {
        return value == PlanLimits.UNLIMITED ? null : value;
    }
}
