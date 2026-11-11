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
 * Resolves {@code Authorization: Bearer <token>} into an authenticated
 * principal named by the user's email — the same principal shape form login
 * produces, so API controllers resolve the user exactly like web controllers.
 * A missing or dead token just leaves the request anonymous; the API filter
 * chain's entry point turns that into a JSON 401.
 *
 * <p>Constructed by {@link SecurityConfig} rather than declared a
 * {@code @Component}, so Boot doesn't also register it in the servlet chain.
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
            tokens.authenticate(token).ifPresent(user -> {
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                        user.getEmail(), null, List.of()));
                SecurityContextHolder.setContext(context);
            });
        }
        chain.doFilter(request, response);
    }
}
