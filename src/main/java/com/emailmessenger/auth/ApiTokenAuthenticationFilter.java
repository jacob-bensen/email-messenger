package com.emailmessenger.auth;

import com.emailmessenger.domain.User;
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
import java.util.Optional;

/**
 * Authenticates {@code /api/**} requests from an {@code Authorization: Bearer}
 * header. The principal name is the user's email, matching the web chain, so
 * controllers resolve the user the same way on both. A missing or dead token
 * leaves the request anonymous and the chain's entry point answers 401.
 *
 * <p>Not a {@code @Component}: it must run only inside the API security chain,
 * and Spring Boot would otherwise also register it on the servlet chain.
 */
class ApiTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer ";

    private final ApiTokenService tokens;

    ApiTokenAuthenticationFilter(ApiTokenService tokens) {
        this.tokens = tokens;
    }

    /** The bearer token on this request, or null when there is none. */
    static String bearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
            return null;
        }
        String token = header.substring(PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = bearerToken(request);
        if (token != null) {
            Optional<User> user = tokens.authenticate(token);
            if (user.isPresent()) {
                // The plaintext token rides along as credentials so logout can
                // revoke exactly the token that authenticated this request.
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                        user.get().getEmail(), token, List.of()));
                SecurityContextHolder.setContext(context);
            }
        }
        chain.doFilter(request, response);
    }
}
