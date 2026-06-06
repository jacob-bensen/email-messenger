package com.emailmessenger.auth;

import com.emailmessenger.domain.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Optional;

/**
 * Endpoints for the email-verification flow.
 *
 * <p>{@code GET /verify-email?token=…} consumes the token (public so a
 * user can verify before signing in) and renders a success or
 * "link expired" screen.
 *
 * <p>{@code POST /verify-email/resend} mints and emails a fresh token
 * for the signed-in user. Used by the unverified-account banner on
 * {@code /threads}; falls back to a flash message because the banner
 * lives on a page we don't own.
 */
@Controller
class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;
    private final UserService userService;

    EmailVerificationController(EmailVerificationService emailVerificationService,
                                UserService userService) {
        this.emailVerificationService = emailVerificationService;
        this.userService = userService;
    }

    @GetMapping("/verify-email")
    String verify(@RequestParam(value = "token", required = false) String token, Model model) {
        Optional<User> verified = emailVerificationService.verify(token);
        if (verified.isEmpty()) {
            model.addAttribute("status", "invalid");
        } else {
            model.addAttribute("status", "verified");
        }
        return "verify-email";
    }

    @PostMapping("/verify-email/resend")
    String resend(Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }
        User user = userService.requireByEmail(principal.getName());
        if (user.isEmailVerified()) {
            redirectAttributes.addFlashAttribute("verifyFlash", "alreadyVerified");
            return "redirect:/threads";
        }
        boolean sent = emailVerificationService.sendVerification(user);
        redirectAttributes.addFlashAttribute("verifyFlash", sent ? "resent" : "resendFailed");
        return "redirect:/threads";
    }
}
