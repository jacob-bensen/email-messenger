package com.emailmessenger.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class MarketingController {

    @GetMapping("/pricing")
    String pricing() {
        return "pricing";
    }
}
