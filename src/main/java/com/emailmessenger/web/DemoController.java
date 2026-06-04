package com.emailmessenger.web;

import com.emailmessenger.service.DemoConversationService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class DemoController {

    private final DemoConversationService demoConversationService;

    DemoController(DemoConversationService demoConversationService) {
        this.demoConversationService = demoConversationService;
    }

    @GetMapping("/demo")
    String demo(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean signedIn = auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal());
        model.addAttribute("conversation", demoConversationService.buildDemo());
        model.addAttribute("demoMode", true);
        model.addAttribute("demoSignedIn", signedIn);
        return "conversation";
    }
}
