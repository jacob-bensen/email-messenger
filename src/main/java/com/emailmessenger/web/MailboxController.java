package com.emailmessenger.web;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.ImapConnectionException;
import com.emailmessenger.email.MailAccountService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/mailboxes")
class MailboxController {

    private final MailAccountService mailAccountService;
    private final UserService userService;

    MailboxController(MailAccountService mailAccountService, UserService userService) {
        this.mailAccountService = mailAccountService;
        this.userService = userService;
    }

    @GetMapping
    String listMailboxes(Principal principal, Model model) {
        User owner = userService.requireByEmail(principal.getName());
        List<MailAccount> accounts = mailAccountService.list(owner);
        model.addAttribute("mailboxes", accounts);
        return "mailboxes/index";
    }

    @GetMapping("/new")
    String newMailbox(Model model) {
        if (!model.containsAttribute("mailboxForm")) {
            model.addAttribute("mailboxForm", new MailboxForm());
        }
        return "mailboxes/new";
    }

    @PostMapping
    String createMailbox(@Valid @ModelAttribute("mailboxForm") MailboxForm form,
                         BindingResult binding,
                         Principal principal) {
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
}
