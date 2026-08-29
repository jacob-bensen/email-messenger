package com.emailmessenger.api;

import com.emailmessenger.billing.PlanLimits;
import com.emailmessenger.domain.Plan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public read-only JSON endpoints consumed by the conexus-mail-mobile
 * client. Contract lives in docs/API.md — this file must match it. Any
 * user-scoped or mutating endpoint added later goes through the same
 * services the Thymeleaf controllers use (see docs/BACKEND.md).
 */
@RestController
@RequestMapping("/api/v1")
class ApiV1Controller {

    private static final String PRODUCT_NAME = "conexus-mail";
    private static final String API_VERSION = "v1";
    private static final String MIN_SUPPORTED_CLIENT = "0.1.0";

    // Monthly / annual-monthly cents per plan, mirroring /pricing and
    // com.emailmessenger.admin.PlanPricing. Kept as a constant here so
    // the public API does not depend on the package-private admin class.
    private static final int PRO_MONTHLY_CENTS = 699;
    private static final int PRO_ANNUAL_MONTHLY_CENTS = 599;

    private final String serverVersion;

    ApiV1Controller(@Value("${app.version:unknown}") String serverVersion) {
        this.serverVersion = serverVersion;
    }

    @GetMapping("/health")
    HealthResponse health() {
        return new HealthResponse("ok");
    }

    @GetMapping("/version")
    VersionResponse version() {
        return new VersionResponse(PRODUCT_NAME, API_VERSION, serverVersion, MIN_SUPPORTED_CLIENT);
    }

    @GetMapping("/plans")
    PlansResponse plans() {
        List<PlanView> plans = List.of(
                planView(Plan.FREE, "Free", 0, 0),
                planView(Plan.PRO, "Pro", PRO_MONTHLY_CENTS, PRO_ANNUAL_MONTHLY_CENTS),
                planView(Plan.BUSINESS, "Business", 0, 0));
        return new PlansResponse("USD", plans);
    }

    private static PlanView planView(Plan plan, String displayName, int monthlyCents, int annualMonthlyCents) {
        PlanLimits limits = PlanLimits.forPlan(plan);
        return new PlanView(
                plan.name(),
                displayName,
                monthlyCents,
                annualMonthlyCents,
                new LimitsView(
                        boxed(limits.mailboxes()),
                        boxed(limits.threads()),
                        boxed(limits.savedSearches())));
    }

    // PlanLimits uses Long.MAX_VALUE as "unlimited"; the wire contract
    // uses JSON null for the same idea.
    private static Long boxed(long value) {
        return value == PlanLimits.UNLIMITED ? null : value;
    }

    record HealthResponse(String status) {}

    record VersionResponse(String name, String apiVersion, String serverVersion, String minSupportedClient) {}

    record PlansResponse(String currency, List<PlanView> plans) {}

    record PlanView(String id, String name, int monthlyCents, int annualMonthlyCents, LimitsView limits) {}

    record LimitsView(Long mailboxes, Long threads, Long savedSearches) {}
}
