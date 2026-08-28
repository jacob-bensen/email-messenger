package com.emailmessenger.web.api;

import com.emailmessenger.domain.ApiToken;

import java.security.Principal;

/**
 * Bearer-token principal placed on {@link org.springframework.security.core.Authentication#getPrincipal()}
 * for authenticated requests under {@code /api/v1/**}. Carries the
 * {@link ApiToken} row so downstream handlers can trace back to the
 * user without a second DB lookup.
 */
public final class ApiPrincipal implements Principal {

    private final ApiToken token;

    ApiPrincipal(ApiToken token) {
        this.token = token;
    }

    @Override
    public String getName() {
        return token.getUser().getEmail();
    }

    public ApiToken token() {
        return token;
    }
}
