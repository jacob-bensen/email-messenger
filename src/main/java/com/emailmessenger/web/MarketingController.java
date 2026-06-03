package com.emailmessenger.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
class MarketingController {

    @GetMapping("/")
    String landing(@RequestParam(name = "utm_source", required = false) String utmSource,
                   Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal())) {
            return "redirect:/threads";
        }
        if (StringUtils.hasText(utmSource)) {
            model.addAttribute("utmSource", utmSource);
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
