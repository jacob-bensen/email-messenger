package com.emailmessenger.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authenticates {@code /api/**} requests from an {@code Authorization: Bearer}
 * token. The principal name is the user's email, matching the web chain, so
 * controllers resolve the user the same way on both paths. Deliberately not a
 * {@code @Component}: it must run only inside the API security chain.
 */
class ApiTokenAuthenticationFilter extends OncePerRequestFilter {

    private final ApiTokenService tokens;

    ApiTokenAuthenticationFilter(ApiTokenService tokens) {
        this.tokens = tokens;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = ApiTokenService.bearerToken(request);
        if (token != null) {
            tokens.resolveEmail(token).ifPresent(email -> {
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(
                        UsernamePasswordAuthenticationToken.authenticated(email, null, List.of()));
                SecurityContextHolder.setContext(context);
            });
        }
        chain.doFilter(request, response);
    }
}
