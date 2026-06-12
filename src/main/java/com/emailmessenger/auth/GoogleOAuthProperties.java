package com.emailmessenger.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Google OAuth client credentials. {@code client-id} drives feature
 * activation — when blank, no {@link
 * org.springframework.security.oauth2.client.registration.ClientRegistrationRepository}
 * bean is created and {@link SecurityConfig} skips {@code oauth2Login()}
 * entirely.
 */
@ConfigurationProperties("auth.google")
public class GoogleOAuthProperties {

    private String clientId = "";
    private String clientSecret = "";
    private String redirectUri = "{baseUrl}/login/oauth2/code/{registrationId}";

    public String getClientId() { return clientId; }
    public String getClientSecret() { return clientSecret; }
    public String getRedirectUri() { return redirectUri; }

    public void setClientId(String clientId) { this.clientId = clientId == null ? "" : clientId.trim(); }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret == null ? "" : clientSecret.trim(); }
    public void setRedirectUri(String redirectUri) {
        if (redirectUri != null && !redirectUri.isBlank()) {
            this.redirectUri = redirectUri.trim();
        }
    }

    public boolean isEnabled() {
        return !clientId.isBlank() && !clientSecret.isBlank();
    }
}
