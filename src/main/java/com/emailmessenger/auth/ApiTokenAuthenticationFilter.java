package com.emailmessenger.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Resolves {@code Authorization: Bearer <token>} on the API chain. The
 * principal name is the user's email — the same shape form login produces —
 * so controllers and services can't tell (and don't care) which chain
 * authenticated the request. A missing or dead token just leaves the request
 * anonymous; the chain's entry point turns that into the JSON 401.
 */
class ApiTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final ApiTokenService tokens;

    ApiTokenAuthenticationFilter(ApiTokenService tokens) {
        this.tokens = tokens;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = bearerToken(request);
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

    /** The raw bearer token on the request, or {@code null}. */
    static String bearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
            return null;
        }
        String token = header.substring(BEARER.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
