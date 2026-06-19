package com.emailmessenger.web;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

/**
 * Serves the three legal pages Stripe (and most EU/CCPA-aware browsers)
 * expect before a paid product goes live. Each route renders the same
 * Thymeleaf layout against a Resource whose location is configurable via
 * {@link LegalProperties} — the shipped classpath boilerplate is good
 * enough to flip Stripe to live mode, but Master can swap in
 * Termly/Iubenda output without touching code.
 */
@Controller
class LegalController {

    private final LegalProperties legalProperties;
    private final ResourceLoader resourceLoader;

    LegalController(LegalProperties legalProperties, ResourceLoader resourceLoader) {
        this.legalProperties = legalProperties;
        this.resourceLoader = resourceLoader;
    }

    @GetMapping("/privacy")
    String privacy(Model model) {
        return render(model, legalProperties.getPrivacy(),
                "/privacy",
                "Privacy Policy — ConexusMail",
                "How ConexusMail collects, stores, and protects the email and account data you entrust to us.");
    }

    @GetMapping("/terms")
    String terms(Model model) {
        return render(model, legalProperties.getTerms(),
                "/terms",
                "Terms of Service — ConexusMail",
                "The terms that govern your use of ConexusMail — accounts, subscriptions, acceptable use, and termination.");
    }

    @GetMapping("/refund")
    String refund(Model model) {
        return render(model, legalProperties.getRefund(),
                "/refund",
                "Refund Policy — ConexusMail",
                "How ConexusMail handles refunds for monthly and annual subscriptions.");
    }

    private String render(Model model, String location,
                          String pagePath, String pageTitle, String pageDescription) {
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("pageDescription", pageDescription);
        model.addAttribute("pagePath", pagePath);
        model.addAttribute("content", loadContent(location));
        return "legal";
    }

    private String loadContent(String location) {
        Resource resource = resourceLoader.getResource(location);
        if (!resource.exists()) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR,
                    "Legal content resource not found: " + location);
        }
        try (InputStream in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR,
                    "Failed to read legal content: " + location, e);
        }
    }
}
