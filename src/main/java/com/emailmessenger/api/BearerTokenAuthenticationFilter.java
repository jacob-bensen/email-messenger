package com.emailmessenger.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Resolves {@code Authorization: Bearer <token>} into an {@link ApiPrincipal}.
 * A missing or invalid token leaves the request anonymous; the API chain's
 * entry point then answers 401 for any endpoint that needs a principal.
 *
 * <p>Deliberately not a Spring bean: Boot would otherwise register it on the
 * servlet chain for every request, website included.
 */
class BearerTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer ";

    private final ApiTokenService tokens;

    BearerTokenAuthenticationFilter(ApiTokenService tokens) {
        this.tokens = tokens;
    }

    /**
     * The chain is stateless, so an async dispatch (an SSE stream completing,
     * §5.24) has no stored security context; re-read the token instead of
     * letting authorization reject the dispatch.
     */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
            tokens.authenticate(header.substring(PREFIX.length()).trim()).ifPresent(principal ->
                    SecurityContextHolder.getContext().setAuthentication(
                            UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of())));
        }
        chain.doFilter(request, response);
    }
}
