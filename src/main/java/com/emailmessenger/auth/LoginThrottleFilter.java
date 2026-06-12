package com.emailmessenger.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Short-circuits {@code POST /login} when the throttle says the email
 * or IP has accumulated too many failures in the window — the request
 * never reaches {@code UsernamePasswordAuthenticationFilter}, so a
 * locked account can't be probed for "is the password right now". The
 * redirect target {@code /login?error=locked} renders a generic
 * "too many attempts — try again later" message; the auth-events row
 * for the {@link com.emailmessenger.domain.AuthEventType#ACCOUNT_LOCKED}
 * was written by the failure listener when the threshold was crossed.
 */
@Component
class LoginThrottleFilter extends OncePerRequestFilter {

    private final LoginThrottleService throttle;

    LoginThrottleFilter(LoginThrottleService throttle) {
        this.throttle = throttle;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod())
                || !"/login".equals(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }
        String email = request.getParameter("email");
        String ip = ClientIp.from(request);
        if (throttle.isLocked(email, ip)) {
            response.sendRedirect(request.getContextPath() + "/login?error=locked");
            return;
        }
        chain.doFilter(request, response);
    }
}
