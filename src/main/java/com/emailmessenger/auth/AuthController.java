package com.emailmessenger.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
class AuthController {

    private final UserService userService;

    AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    String registerPage(Model model) {
        if (!model.containsAttribute("registrationForm")) {
            model.addAttribute("registrationForm", new RegistrationForm());
        }
        return "register";
    }

    @PostMapping("/register")
    String register(@Valid @ModelAttribute("registrationForm") RegistrationForm form,
                    BindingResult binding,
                    HttpServletRequest request) throws ServletException {
        if (binding.hasErrors()) {
            return "register";
        }
        try {
            userService.register(form.getEmail(), form.getPassword(), form.getDisplayName());
        } catch (EmailAlreadyRegisteredException e) {
            binding.rejectValue("email", "email.taken",
                    "An account with that email already exists.");
            return "register";
        }
        // Auto-login after registration so the user lands on /threads, not /login.
        request.login(UserService.normalizeEmail(form.getEmail()), form.getPassword());
        return "redirect:/threads";
    }
}
