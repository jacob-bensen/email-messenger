package com.emailmessenger.auth;

import com.emailmessenger.billing.BillingException;
import com.emailmessenger.billing.BillingPeriod;
import com.emailmessenger.billing.BillingService;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * After login, if the form carried a {@code plan} param, start a Stripe
 * Checkout session and redirect there. Otherwise fall back to the standard
 * saved-request / default-URL behaviour so users redirected to login from
 * a protected page still land where they were going.
 */
@Component
class PlanCheckoutSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(PlanCheckoutSuccessHandler.class);

    private final UserService userService;
    private final BillingService billingService;
    private final HttpSessionRequestCache requestCache = new HttpSessionRequestCache();

    PlanCheckoutSuccessHandler(UserService userService, BillingService billingService) {
        this.userService = userService;
        this.billingService = billingService;
        setDefaultTargetUrl("/threads");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String checkoutUrl = checkoutUrlFor(request, authentication);
        if (checkoutUrl != null) {
            // A pre-login saved request (e.g. /threads) would otherwise win
            // over our redirect on the next handler in the chain.
            requestCache.removeRequest(request, response);
            getRedirectStrategy().sendRedirect(request, response, checkoutUrl);
            return;
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }

    private String checkoutUrlFor(HttpServletRequest request, Authentication authentication) {
        String planParam = request.getParameter("plan");
        if (!StringUtils.hasText(planParam)) {
            return null;
        }
        Plan plan;
        try {
            plan = Plan.parse(planParam);
        } catch (IllegalArgumentException e) {
            log.warn("Ignoring unknown plan '{}' at login for {}", planParam, authentication.getName());
            return null;
        }
        BillingPeriod period = BillingPeriod.parse(request.getParameter("billing"));
        try {
            User user = userService.requireByEmail(authentication.getName());
            return billingService.startCheckout(user, plan, period);
        } catch (BillingException e) {
            log.warn("Skipping checkout after login for plan {}: {}", plan, e.getMessage());
            return null;
        }
    }
}
