package com.emailmessenger.web;

import com.emailmessenger.repository.WaitlistEntryRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class LandingController {

    private final WaitlistEntryRepository waitlistEntryRepository;

    LandingController(WaitlistEntryRepository waitlistEntryRepository) {
        this.waitlistEntryRepository = waitlistEntryRepository;
    }

    @GetMapping("/")
    String home(Model model) {
        model.addAttribute("waitlistCount", waitlistEntryRepository.count());
        return "index";
    }
}
