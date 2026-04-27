package com.emailmessenger.web;

import com.emailmessenger.service.Conversation;
import com.emailmessenger.service.DemoService;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
class DemoController {

    private final DemoService demoService;

    DemoController(DemoService demoService) {
        this.demoService = demoService;
    }

    @GetMapping("/demo")
    String demo(Model model) {
        model.addAttribute("demoThreads", demoService.listThreads());
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("yesterday", LocalDate.now().minusDays(1));
        return "demo";
    }

    @GetMapping("/demo/{id}")
    String demoConversation(@PathVariable int id, Model model) {
        Conversation conversation = demoService.getConversation(id);
        if (conversation == null) {
            throw new NoSuchElementException("Demo thread not found: " + id);
        }
        model.addAttribute("conversation", conversation);
        model.addAttribute("isDemo", true);
        return "conversation";
    }
}
