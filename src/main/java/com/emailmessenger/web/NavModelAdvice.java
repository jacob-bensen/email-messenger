package com.emailmessenger.web;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.billing.BillingService;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.MailAccountService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

/**
 * Supplies the left sidebar's data (connected mailboxes, profile name, billing
 * availability, and which section is active) to every authenticated page, so the
 * persistent nav renders identically across the inbox, sender chat, mailboxes,
 * and account views without each controller repeating the wiring.
 */
@ControllerAdvice
class NavModelAdvice {

    private final UserService userService;
    private final MailAccountService mailAccountService;
    private final BillingService billingService;

    NavModelAdvice(UserService userService,
                   MailAccountService mailAccountService,
                   BillingService billingService) {
        this.userService = userService;
        this.mailAccountService = mailAccountService;
        this.billingService = billingService;
    }

    @ModelAttribute
    void populateNav(Principal principal, HttpServletRequest request, Model model) {
        model.addAttribute("navActive", activeSection(request.getRequestURI()));
        if (principal == null) {
            return;
        }
        User user = userService.findByEmail(principal.getName()).orElse(null);
        if (user == null) {
            return;
        }
        String name = (user.getDisplayName() != null && !user.getDisplayName().isBlank())
                ? user.getDisplayName() : user.getEmail();
        model.addAttribute("navProfileName", name);
        model.addAttribute("navMailboxes", mailAccountService.list(user).stream()
                .map(a -> new NavMailbox(a.getId(), a.getUsername()))
                .toList());
        model.addAttribute("navHasBilling", billingService.hasManagedBilling(user));
    }

    private static String activeSection(String uri) {
        if (uri == null) {
            return "";
        }
        if (uri.equals("/threads") || uri.startsWith("/threads/") || uri.startsWith("/senders")) {
            return "inbox";
        }
        if (uri.startsWith("/mailboxes")) {
            return "mailboxes";
        }
        if (uri.startsWith("/account")) {
            return "account";
        }
        if (uri.startsWith("/pricing")) {
            return "pricing";
        }
        return "";
    }

    record NavMailbox(Long id, String address) {}
}
