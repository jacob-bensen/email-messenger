package com.emailmessenger.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class MarketingController {

    @GetMapping("/")
    String landing() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal())) {
            return "redirect:/threads";
        }
        return "landing";
    }

    @GetMapping("/pricing")
    String pricing() {
        return "pricing";
    }
}
