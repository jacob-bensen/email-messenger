package com.emailmessenger.auth;

import com.emailmessenger.billing.BillingException;
import com.emailmessenger.billing.BillingService;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final BillingService billingService;

    AuthController(UserService userService, BillingService billingService) {
        this.userService = userService;
        this.billingService = billingService;
    }

    @GetMapping("/login")
    String loginPage(@RequestParam(name = "plan", required = false) String plan, Model model) {
        if (StringUtils.hasText(plan)) {
            model.addAttribute("plan", plan);
        }
        return "login";
    }

    @GetMapping("/register")
    String registerPage(@RequestParam(name = "plan", required = false) String plan, Model model) {
        if (!model.containsAttribute("registrationForm")) {
            model.addAttribute("registrationForm", new RegistrationForm());
        }
        if (StringUtils.hasText(plan)) {
            model.addAttribute("plan", plan);
        }
        return "register";
    }

    @PostMapping("/register")
    String register(@Valid @ModelAttribute("registrationForm") RegistrationForm form,
                    BindingResult binding,
                    @RequestParam(name = "plan", required = false) String plan,
                    Model model,
                    HttpServletRequest request) throws ServletException {
        if (StringUtils.hasText(plan)) {
            model.addAttribute("plan", plan);
        }
        if (binding.hasErrors()) {
            return "register";
        }
        try {
            userService.register(form.getEmail(), form.getPassword(), form.getDisplayName());
        } catch (EmailAlreadyRegisteredException e) {
            binding.rejectValue("email", "email.taken",
                    "An account with that email already exists.");
            return "register";
        }
        // Auto-login after registration so the user lands inside the app,
        // not on the login screen.
        request.login(UserService.normalizeEmail(form.getEmail()), form.getPassword());

        String checkoutRedirect = checkoutRedirectFor(plan, form.getEmail());
        return checkoutRedirect != null ? checkoutRedirect : "redirect:/threads";
    }

    /**
     * Translates a plan query param into a Stripe Checkout redirect. Returns
     * null for missing/unsupported plans so callers fall through to /threads
     * — a tampered or unknown plan must not strand a freshly-signed-up user
     * on an error page.
     */
    private String checkoutRedirectFor(String plan, String email) {
        if (!StringUtils.hasText(plan)) {
            return null;
        }
        Plan parsed;
        try {
            parsed = Plan.parse(plan);
        } catch (IllegalArgumentException e) {
            log.warn("Ignoring unknown plan '{}' on register for {}", plan, email);
            return null;
        }
        try {
            User user = userService.requireByEmail(email);
            String url = billingService.startCheckout(user, parsed);
            return "redirect:" + url;
        } catch (BillingException e) {
            log.warn("Skipping checkout after register for plan {}: {}", parsed, e.getMessage());
            return null;
        }
    }
}
