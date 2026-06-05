package com.emailmessenger.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
class MarketingController {

    private final LandingProperties landingProperties;

    MarketingController(LandingProperties landingProperties) {
        this.landingProperties = landingProperties;
    }

    @GetMapping("/")
    String landing(@RequestParam(name = "utm_source", required = false) String utmSource,
                   @RequestParam(name = "demo", required = false) String demo,
                   Model model) {
        if (StringUtils.hasText(demo)) {
            return StringUtils.hasText(utmSource)
                    ? "redirect:/demo?utm_source=" + URLEncoder.encode(utmSource, StandardCharsets.UTF_8)
                    : "redirect:/demo";
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal())) {
            return "redirect:/threads";
        }
        if (StringUtils.hasText(utmSource)) {
            model.addAttribute("utmSource", utmSource);
        }
        LandingVideo demoVideo = LandingVideo.from(landingProperties.getVideo());
        if (demoVideo != null) {
            model.addAttribute("demoVideo", demoVideo);
        }
        return "landing";
    }

    @GetMapping("/pricing")
    String pricing(@RequestParam(name = "utm_source", required = false) String utmSource,
                   Model model) {
        if (StringUtils.hasText(utmSource)) {
            model.addAttribute("utmSource", utmSource);
        }
        return "pricing";
    }
}
