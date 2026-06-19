package com.emailmessenger.web;

import com.emailmessenger.auth.GoogleOAuthProperties;
import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.ImapConnectionException;
import com.emailmessenger.email.MailAccountService;
import com.emailmessenger.email.MailboxPollingService;
import com.emailmessenger.repository.MailAccountRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Controller
@RequestMapping("/mailboxes")
class MailboxController {

    private final MailAccountService mailAccountService;
    private final MailAccountRepository mailAccountRepository;
    private final MailboxPollingService pollingService;
    private final UserService userService;
    private final GoogleOAuthProperties googleOAuthProperties;

    MailboxController(MailAccountService mailAccountService,
                      MailAccountRepository mailAccountRepository,
                      MailboxPollingService pollingService,
                      UserService userService,
                      GoogleOAuthProperties googleOAuthProperties) {
        this.mailAccountService = mailAccountService;
        this.mailAccountRepository = mailAccountRepository;
        this.pollingService = pollingService;
        this.userService = userService;
        this.googleOAuthProperties = googleOAuthProperties;
    }

    @GetMapping
    String listMailboxes(Principal principal, Model model) {
        User owner = userService.requireByEmail(principal.getName());
        LocalDateTime now = LocalDateTime.now();
        List<MailboxView> mailboxes = mailAccountService.list(owner).stream()
                .map(a -> MailboxView.from(a, now))
                .toList();
        model.addAttribute("mailboxes", mailboxes);
        return "mailboxes/index";
    }

    @GetMapping("/new")
    String newMailbox(@RequestParam(value = "provider", required = false) String providerSlug,
                      Model model) {
        Optional<MailboxProvider> selected = MailboxProvider.fromSlug(providerSlug);
        model.addAttribute("providers", MailboxProvider.values());
        model.addAttribute("googleOAuthEnabled", googleOAuthProperties.isEnabled());
        if (selected.isEmpty()) {
            return "mailboxes/new";
        }
        MailboxProvider provider = selected.get();
        if (!model.containsAttribute("mailboxForm")) {
            MailboxForm form = new MailboxForm();
            form.setHost(provider.getHost());
            form.setPort(provider.getPort());
            form.setSsl(provider.isSsl());
            form.setProvider(provider.getSlug());
            model.addAttribute("mailboxForm", form);
        }
        model.addAttribute("provider", provider);
        return "mailboxes/new";
    }

    @PostMapping
    String createMailbox(@Valid @ModelAttribute("mailboxForm") MailboxForm form,
                         BindingResult binding,
                         Model model,
                         Principal principal) {
        Optional<MailboxProvider> selected = MailboxProvider.fromSlug(form.getProvider());
        selected.ifPresent(p -> model.addAttribute("provider", p));
        model.addAttribute("providers", MailboxProvider.values());
        model.addAttribute("googleOAuthEnabled", googleOAuthProperties.isEnabled());
        if (binding.hasErrors()) {
            return "mailboxes/new";
        }
        User owner = userService.requireByEmail(principal.getName());
        try {
            // PlanLimitExceededException intentionally propagates to the
            // GlobalExceptionHandler, which surfaces the upgrade modal on /threads.
            MailAccount saved = mailAccountService.connect(
                    owner, form.getHost(), form.getPort(), form.isSsl(),
                    form.getUsername(), form.getPassword());
            // Sync-error path: send the user to /mailboxes so they see the
            // row with the error indicator instead of an empty /threads.
            return saved.getLastSyncError() == null
                    ? "redirect:/threads"
                    : "redirect:/mailboxes";
        } catch (ImapConnectionException e) {
            binding.reject("imap.connect.failed", new Object[]{e.getMessage()},
                    "Could not connect to that mailbox: " + e.getMessage());
            return "mailboxes/new";
        }
    }

    @PostMapping("/{id}/sync")
    String syncMailbox(@PathVariable Long id,
                       Principal principal,
                       RedirectAttributes redirectAttributes) {
        User owner = userService.requireByEmail(principal.getName());
        // Ownership check: a NoSuchElementException for a foreign or unknown id
        // surfaces as a 404 through GlobalExceptionHandler — same shape as
        // ThreadController.viewConversation.
        MailAccount account = mailAccountRepository.findByIdAndUser(id, owner)
                .orElseThrow(NoSuchElementException::new);
        pollingService.pollOne(account.getId());
        // Reload so we see whatever pollOne wrote — lastSyncedAt + cleared
        // error on success, or lastSyncError populated on a connect failure.
        MailAccount reloaded = mailAccountRepository.findById(account.getId())
                .orElseThrow(NoSuchElementException::new);
        if (reloaded.getLastSyncError() != null) {
            redirectAttributes.addFlashAttribute("syncError",
                    "Sync failed: " + reloaded.getLastSyncError());
        } else {
            redirectAttributes.addFlashAttribute("syncMessage",
                    "Mailbox synced.");
        }
        return "redirect:/mailboxes";
    }

    @PostMapping("/{id}/delete")
    String deleteMailbox(@PathVariable Long id,
                         Principal principal,
                         RedirectAttributes redirectAttributes) {
        User owner = userService.requireByEmail(principal.getName());
        // A foreign or unknown id 404s, same posture as the sync endpoint.
        if (!mailAccountService.delete(owner, id)) {
            throw new NoSuchElementException();
        }
        redirectAttributes.addFlashAttribute("syncMessage", "Mailbox removed.");
        return "redirect:/mailboxes";
    }
}
