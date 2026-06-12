package com.emailmessenger.web;

import com.emailmessenger.domain.WaitlistEntry;
import com.emailmessenger.repository.WaitlistEntryRepository;
import com.emailmessenger.service.WaitlistReferralService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/waitlist")
class WaitlistController {

    private final WaitlistEntryRepository waitlistRepo;
    private final WaitlistReferralService referralService;
    private final String baseUrl;

    WaitlistController(WaitlistEntryRepository waitlistRepo,
                       WaitlistReferralService referralService,
                       @Value("${app.base-url:https://mailaim.app}") String baseUrl) {
        this.waitlistRepo = waitlistRepo;
        this.referralService = referralService;
        this.baseUrl = stripTrailingSlash(baseUrl);
    }

    @GetMapping
    String showForm(@RequestParam(value = "ref", required = false) String ref, Model model) {
        WaitlistForm form = new WaitlistForm();
        if (ref != null) {
            form.setRef(ref);
        }
        model.addAttribute("waitlistForm", form);
        model.addAttribute("waitlistCount", waitlistRepo.count());
        model.addAttribute("baseUrl", baseUrl);
        return "waitlist";
    }

    @PostMapping
    String submit(@Valid @ModelAttribute("waitlistForm") WaitlistForm form,
                  BindingResult bindingResult,
                  Model model,
                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("waitlistCount", waitlistRepo.count());
            model.addAttribute("baseUrl", baseUrl);
            return "waitlist";
        }
        if (waitlistRepo.existsByEmail(form.getEmail())) {
            redirectAttributes.addFlashAttribute("alreadyJoined", true);
            shareAttributesFor(form.getEmail(), redirectAttributes);
            return "redirect:/waitlist?joined";
        }
        WaitlistEntry saved = null;
        try {
            saved = waitlistRepo.save(new WaitlistEntry(form.getEmail()));
        } catch (DataIntegrityViolationException ignored) {
            // concurrent duplicate — fall through and treat as already joined
        }
        referralService.creditReferrer(form.getRef(), form.getEmail());
        redirectAttributes.addFlashAttribute("joined", true);
        shareAttributesFor(saved == null ? form.getEmail() : saved.getEmail(), redirectAttributes);
        return "redirect:/waitlist?success";
    }

    private void shareAttributesFor(String email, RedirectAttributes redirectAttributes) {
        Optional<WaitlistEntry> entry = waitlistRepo.findByEmail(email);
        if (entry.isEmpty()) {
            return;
        }
        WaitlistEntry e = entry.get();
        if (e.getReferralToken() != null) {
            redirectAttributes.addFlashAttribute("referralUrl",
                    baseUrl + "/waitlist?ref=" + e.getReferralToken());
        }
        redirectAttributes.addFlashAttribute("position", referralService.effectivePosition(e));
        redirectAttributes.addFlashAttribute("referralsCount", e.getReferralsCount());
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
