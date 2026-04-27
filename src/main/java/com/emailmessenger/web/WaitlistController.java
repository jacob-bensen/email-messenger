package com.emailmessenger.web;

import com.emailmessenger.domain.WaitlistEntry;
import com.emailmessenger.repository.WaitlistEntryRepository;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/waitlist")
class WaitlistController {

    private final WaitlistEntryRepository waitlistRepo;

    WaitlistController(WaitlistEntryRepository waitlistRepo) {
        this.waitlistRepo = waitlistRepo;
    }

    @GetMapping
    String showForm(Model model) {
        model.addAttribute("waitlistForm", new WaitlistForm());
        return "waitlist";
    }

    @PostMapping
    String submit(@Valid @ModelAttribute("waitlistForm") WaitlistForm form,
                  BindingResult bindingResult,
                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "waitlist";
        }
        if (waitlistRepo.existsByEmail(form.getEmail())) {
            redirectAttributes.addFlashAttribute("alreadyJoined", true);
            return "redirect:/waitlist?joined";
        }
        try {
            waitlistRepo.save(new WaitlistEntry(form.getEmail()));
        } catch (DataIntegrityViolationException ignored) {
            // concurrent duplicate — treat as already joined
        }
        redirectAttributes.addFlashAttribute("joined", true);
        return "redirect:/waitlist?success";
    }
}
