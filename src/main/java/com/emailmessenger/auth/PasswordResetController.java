package com.emailmessenger.auth;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Unauthenticated endpoints for the "forgot password" flow.
 *
 * <p>{@code GET /password/forgot} renders the email-entry form.
 * {@code POST /password/forgot} issues a reset email (silently no-op
 * for unknown / disabled accounts) and renders the same generic
 * confirmation either way, so the response can't be used to enumerate
 * which addresses are registered.
 *
 * <p>{@code GET /password/reset?token=…} validates the token and either
 * renders the new-password form or a "link expired" page. {@code POST
 * /password/reset} consumes the token, sets the new password, and
 * redirects to {@code /login?reset} so the user signs in fresh with
 * the new credentials.
 */
@Controller
class PasswordResetController {

    private final PasswordResetService passwordResetService;

    PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/password/forgot")
    String forgotForm() {
        return "password/forgot";
    }

    @PostMapping("/password/forgot")
    String requestReset(@RequestParam(value = "email", required = false) String email,
                        Model model) {
        passwordResetService.requestReset(email);
        model.addAttribute("status", "sent");
        return "password/forgot";
    }

    @GetMapping("/password/reset")
    String resetForm(@RequestParam(value = "token", required = false) String token,
                     Model model) {
        if (passwordResetService.findUserForValidToken(token).isEmpty()) {
            model.addAttribute("status", "invalid");
            return "password/reset";
        }
        model.addAttribute("token", token);
        return "password/reset";
    }

    @PostMapping("/password/reset")
    String consumeReset(@RequestParam(value = "token", required = false) String token,
                        @RequestParam(value = "password", required = false) String password,
                        Model model) {
        if (password == null || password.length() < 8) {
            model.addAttribute("token", token);
            model.addAttribute("error", "tooShort");
            return "password/reset";
        }
        if (passwordResetService.consumeToken(token, password).isEmpty()) {
            model.addAttribute("status", "invalid");
            return "password/reset";
        }
        return "redirect:/login?reset";
    }
}
