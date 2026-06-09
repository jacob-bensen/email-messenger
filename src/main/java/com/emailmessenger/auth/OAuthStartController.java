package com.emailmessenger.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Entry point for the "Continue with Google" button. Captures
 * {@code plan}, {@code billing}, and {@code utm_source} from the click
 * URL into the session before handing off to Spring Security's OAuth2
 * authorization endpoint — those params would otherwise vanish on the
 * Google round-trip.
 */
@Controller
class OAuthStartController {

    private final OAuthIntentStore intents;

    OAuthStartController(OAuthIntentStore intents) {
        this.intents = intents;
    }

    @GetMapping("/auth/google/start")
    String startGoogle(@RequestParam(name = "plan", required = false) String plan,
                       @RequestParam(name = "billing", required = false) String billing,
                       @RequestParam(name = "utm_source", required = false) String utmSource,
                       HttpServletRequest request) {
        intents.store(request, plan, billing, utmSource);
        return "redirect:/oauth2/authorization/google";
    }
}
