package com.emailmessenger.billing;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

@Controller
class BillingController {

    private final BillingService billingService;
    private final UserService userService;

    BillingController(BillingService billingService, UserService userService) {
        this.billingService = billingService;
        this.userService = userService;
    }

    @PostMapping("/billing/checkout")
    String startCheckout(@RequestParam("plan") String planParam, Principal principal) {
        Plan plan = Plan.parse(planParam);
        User user = userService.requireByEmail(principal.getName());
        String checkoutUrl = billingService.startCheckout(user, plan);
        return "redirect:" + checkoutUrl;
    }

    @GetMapping("/billing/success")
    String success(@RequestParam(name = "session_id", required = false) String sessionId,
                   Model model) {
        model.addAttribute("sessionId", sessionId);
        return "billing/success";
    }

    @GetMapping("/billing/cancel")
    String canceled() {
        return "redirect:/pricing?canceled=1";
    }
}
