package com.emailmessenger.web.api;

import com.emailmessenger.domain.ApiToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Servlet filter that pulls {@code Authorization: Bearer <token>} off the
 * request, resolves it through {@link ApiTokenService}, and installs an
 * authenticated principal for the downstream API filter chain.
 *
 * <p>Missing or malformed headers pass through unauthenticated — the
 * chain's {@code anyRequest().authenticated()} rule + the JSON entry
 * point then produce the 401 response.
 */
final class ApiTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final ApiTokenService tokens;

    ApiTokenAuthenticationFilter(ApiTokenService tokens) {
        this.tokens = tokens;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String plaintext = header.substring(BEARER_PREFIX.length()).trim();
            Optional<ApiToken> resolved = tokens.resolve(plaintext);
            if (resolved.isPresent()) {
                ApiPrincipal principal = new ApiPrincipal(resolved.get());
                var auth = new UsernamePasswordAuthenticationToken(
                        principal, plaintext,
                        List.of(new SimpleGrantedAuthority("ROLE_API_USER")));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(request, response);
    }
}
