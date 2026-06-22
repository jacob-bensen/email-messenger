package com.emailmessenger.auth;

import com.emailmessenger.billing.BillingPeriod;
import com.emailmessenger.domain.Plan;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Locale;

/**
 * Carries plan, billing, and utm_source across the Google OAuth round-trip
 * via the HTTP session. The visitor's intent (e.g. "wanted Pro annual,
 * came from utm=producthunt") would otherwise be lost when Spring Security
 * redirects to Google — Spring's authorization-request URL has no slot for
 * our params and Google strips anything it doesn't understand.
 */
@Component
class OAuthIntentStore {

    private static final String ATTR_PLAN = "conexusmail.oauth.plan";
    private static final String ATTR_BILLING = "conexusmail.oauth.billing";
    private static final String ATTR_UTM = "conexusmail.oauth.utm_source";
    private static final int UTM_MAX = 64;

    void store(HttpServletRequest request, String plan, String billing, String utmSource) {
        HttpSession session = request.getSession(true);
        String planValue = sanitizedPlan(plan);
        String billingValue = sanitizedBilling(billing);
        String utmValue = sanitizedUtm(utmSource);
        setOrClear(session, ATTR_PLAN, planValue);
        setOrClear(session, ATTR_BILLING, billingValue);
        setOrClear(session, ATTR_UTM, utmValue);
    }

    OAuthIntent peek(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return OAuthIntent.EMPTY;
        }
        return new OAuthIntent(
                (String) session.getAttribute(ATTR_PLAN),
                (String) session.getAttribute(ATTR_BILLING),
                (String) session.getAttribute(ATTR_UTM));
    }

    /**
     * Peek at the intent associated with the request currently bound to
     * this thread. Used by {@link GoogleOidcUserService}, which is
     * invoked deep inside Spring Security's OAuth callback and has no
     * direct request reference.
     */
    OAuthIntent peekCurrent() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes servlet)) {
            return OAuthIntent.EMPTY;
        }
        return peek(servlet.getRequest());
    }

    OAuthIntent consume(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return OAuthIntent.EMPTY;
        }
        OAuthIntent intent = new OAuthIntent(
                (String) session.getAttribute(ATTR_PLAN),
                (String) session.getAttribute(ATTR_BILLING),
                (String) session.getAttribute(ATTR_UTM));
        session.removeAttribute(ATTR_PLAN);
        session.removeAttribute(ATTR_BILLING);
        session.removeAttribute(ATTR_UTM);
        return intent;
    }

    private static String sanitizedPlan(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return Plan.parse(raw).name().toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String sanitizedBilling(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        return BillingPeriod.parse(raw).paramValue();
    }

    private static String sanitizedUtm(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.length() > UTM_MAX ? trimmed.substring(0, UTM_MAX) : trimmed;
    }

    private static void setOrClear(HttpSession session, String key, String value) {
        if (value == null) {
            session.removeAttribute(key);
        } else {
            session.setAttribute(key, value);
        }
    }
}
