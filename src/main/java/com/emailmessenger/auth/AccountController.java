package com.emailmessenger.auth;

import com.emailmessenger.domain.AuthEvent;
import com.emailmessenger.domain.Subscription;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.SubscriptionRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

/**
 * Authenticated self-serve account page.
 *
 * <p>{@code GET /account} shows current email/display name + the change
 * forms. {@code POST /account/password} swaps the bcrypt hash and
 * flashes a status outcome. {@code POST /account/email} swaps the
 * username on the {@code users} row, ends the current session, and
 * redirects to {@code /login?email-changed} so the user signs back in
 * as the new address (and a verification mail is already on the way).
 */
@Controller
class AccountController {

    static final int RECENT_ACTIVITY_LIMIT = 10;

    private final AccountService accountService;
    private final UserService userService;
    private final AuthEventService authEventService;
    private final SubscriptionRepository subscriptions;

    AccountController(AccountService accountService,
                      UserService userService,
                      AuthEventService authEventService,
                      SubscriptionRepository subscriptions) {
        this.accountService = accountService;
        this.userService = userService;
        this.authEventService = authEventService;
        this.subscriptions = subscriptions;
    }

    @GetMapping("/account")
    String account(Principal principal, Model model) {
        User user = userService.requireByEmail(principal.getName());
        model.addAttribute("currentEmail", user.getEmail());
        model.addAttribute("displayName", user.getDisplayName());
        model.addAttribute("emailVerified", user.isEmailVerified());
        Subscription sub = subscriptions.findByUser(user).orElse(null);
        AccountBillingSummary billing = AccountBillingSummary.from(sub);
        model.addAttribute("billing", billing);
        List<AuthEvent> recentActivity = authEventService.recentFor(user, RECENT_ACTIVITY_LIMIT);
        model.addAttribute("recentActivity", recentActivity);
        return "account";
    }

    @PostMapping("/account/password")
    String changePassword(@RequestParam(value = "currentPassword", required = false) String currentPassword,
                          @RequestParam(value = "newPassword", required = false) String newPassword,
                          Principal principal,
                          RedirectAttributes redirectAttributes) {
        User user = userService.requireByEmail(principal.getName());
        AccountService.PasswordChangeOutcome outcome =
                accountService.changePassword(user, currentPassword, newPassword);
        redirectAttributes.addFlashAttribute("passwordOutcome", outcome.name());
        return "redirect:/account";
    }

    @PostMapping("/account/email")
    String changeEmail(@RequestParam(value = "currentPassword", required = false) String currentPassword,
                       @RequestParam(value = "newEmail", required = false) String newEmail,
                       Principal principal,
                       HttpServletRequest request,
                       RedirectAttributes redirectAttributes) {
        User user = userService.requireByEmail(principal.getName());
        AccountService.EmailChangeOutcome outcome =
                accountService.changeEmail(user, currentPassword, newEmail);
        if (outcome == AccountService.EmailChangeOutcome.OK) {
            // Session principal still points at the old username; let the
            // user re-authenticate as the new address.
            SecurityContextHolder.clearContext();
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            return "redirect:/login?email-changed";
        }
        redirectAttributes.addFlashAttribute("emailOutcome", outcome.name());
        return "redirect:/account";
    }
}
