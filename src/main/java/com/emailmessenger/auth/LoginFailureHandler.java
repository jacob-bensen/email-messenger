package com.emailmessenger.auth;

import com.emailmessenger.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Routes a failed form login to {@code /login?error=google} when the
 * submitted email matches a Google-linked row, and to plain
 * {@code /login?error} otherwise. The Thymeleaf login template renders
 * a "Did you mean to sign in with Google?" hint on the
 * {@code error=google} branch so a Google user who muscle-memoried their
 * password into the wrong field gets pointed at the right button instead
 * of bouncing off "Incorrect email or password" until they give up.
 *
 * <p>Only rows with a {@code google_subject} stamped are surfaced this
 * way — a brute-forcer pinging random emails still gets the generic
 * "Incorrect email or password" path, and the lockout filter still
 * short-circuits before this handler runs.
 */
@Component
class LoginFailureHandler implements AuthenticationFailureHandler {

    private final UserRepository users;

    LoginFailureHandler(UserRepository users) {
        this.users = users;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String target = "/login?error";
        String email = request.getParameter("email");
        if (email != null && !email.isBlank()) {
            String normalized = UserService.normalizeEmail(email);
            boolean googleLinked = users.findByEmail(normalized)
                    .map(u -> u.getGoogleSubject() != null)
                    .orElse(false);
            if (googleLinked) {
                target = "/login?error=google";
            }
        }
        response.sendRedirect(request.getContextPath() + target);
    }
}
