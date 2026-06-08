package com.emailmessenger.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
class AdminRevenueController {

    private final AdminAuthorizer authorizer;
    private final RevenueMetricsService metricsService;

    AdminRevenueController(AdminAuthorizer authorizer, RevenueMetricsService metricsService) {
        this.authorizer = authorizer;
        this.metricsService = metricsService;
    }

    @GetMapping("/admin/revenue")
    String revenue(Principal principal, Model model) {
        authorizer.requireAdmin(principal.getName());
        RevenueMetrics metrics = metricsService.snapshot();
        model.addAttribute("metrics", metrics);
        return "admin/revenue";
    }
}
