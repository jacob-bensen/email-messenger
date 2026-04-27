package com.emailmessenger.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class LegalController {

    @GetMapping("/privacy")
    String privacy() {
        return "privacy";
    }

    @GetMapping("/terms")
    String terms() {
        return "terms";
    }

    @GetMapping("/refund")
    String refund() {
        return "refund";
    }
}
