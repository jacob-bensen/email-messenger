package com.emailmessenger.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
class AdminRevenueController {

    private final AdminAuthorizer authorizer;
    private final RevenueMetricsService metricsService;
    private final FunnelMetricsService funnelService;
    private final TrialEndConversionMetricsService trialEndMetricsService;
    private final OnboardingFunnelMetricsService onboardingFunnelService;
    private final TeamAdoptionMetricsService teamAdoptionService;
    private final ChurnMetricsService churnMetricsService;
    private final AtRiskRetentionService atRiskRetentionService;
    private final BillingPeriodBackfillService backfillService;

    AdminRevenueController(AdminAuthorizer authorizer,
                           RevenueMetricsService metricsService,
                           FunnelMetricsService funnelService,
                           TrialEndConversionMetricsService trialEndMetricsService,
                           OnboardingFunnelMetricsService onboardingFunnelService,
                           TeamAdoptionMetricsService teamAdoptionService,
                           ChurnMetricsService churnMetricsService,
                           AtRiskRetentionService atRiskRetentionService,
                           BillingPeriodBackfillService backfillService) {
        this.authorizer = authorizer;
        this.metricsService = metricsService;
        this.funnelService = funnelService;
        this.trialEndMetricsService = trialEndMetricsService;
        this.onboardingFunnelService = onboardingFunnelService;
        this.teamAdoptionService = teamAdoptionService;
        this.churnMetricsService = churnMetricsService;
        this.atRiskRetentionService = atRiskRetentionService;
        this.backfillService = backfillService;
    }

    @GetMapping("/admin/revenue")
    String revenue(Principal principal, Model model) {
        authorizer.requireAdmin(principal.getName());
        RevenueMetrics metrics = metricsService.snapshot();
        FunnelMetrics funnel = funnelService.snapshot();
        TrialEndConversionMetrics trialEnd = trialEndMetricsService.snapshot();
        OnboardingFunnelMetrics onboardingFunnel = onboardingFunnelService.snapshot();
        TeamAdoptionMetrics teamAdoption = teamAdoptionService.snapshot();
        ChurnMetrics churn = churnMetricsService.snapshot();
        AtRiskRetentionMetrics atRiskRetention = atRiskRetentionService.snapshot();
        model.addAttribute("metrics", metrics);
        model.addAttribute("funnel", funnel);
        model.addAttribute("trialEnd", trialEnd);
        model.addAttribute("onboardingFunnel", onboardingFunnel);
        model.addAttribute("teamAdoption", teamAdoption);
        model.addAttribute("churn", churn);
        model.addAttribute("atRiskRetention", atRiskRetention);
        return "admin/revenue";
    }

    @PostMapping("/admin/revenue/reconcile-billing-period")
    String reconcileBillingPeriod(Principal principal, RedirectAttributes attrs) {
        authorizer.requireAdmin(principal.getName());
        BillingPeriodBackfillResult result = backfillService.reconcile();
        attrs.addFlashAttribute("reconcile", result);
        return "redirect:/admin/revenue";
    }
}
