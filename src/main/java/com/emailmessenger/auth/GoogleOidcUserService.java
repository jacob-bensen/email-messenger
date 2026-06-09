package com.emailmessenger.auth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Bridges Spring Security's OIDC login with our {@link User} table.
 *
 * <p>Spring's default {@link OidcUserService} loads the OIDC user from
 * Google's id_token + userinfo endpoint. We delegate to it, then call
 * {@link OAuth2ProvisioningService#provisionFromGoogle} so a brand-new
 * Google email materialises as a {@code users} row before the success
 * handler tries to look it up. We then rebuild the principal so its
 * {@code getName()} returns the email — every controller and audit
 * listener in the app keys off {@code Authentication.getName()}, and a
 * raw Google {@code sub} claim there would break those lookups.
 */
@Component
class GoogleOidcUserService extends OidcUserService {

    private final OAuth2ProvisioningService provisioner;
    private final OAuthIntentStore intents;

    GoogleOidcUserService(OAuth2ProvisioningService provisioner,
                          OAuthIntentStore intents) {
        this.provisioner = provisioner;
        this.intents = intents;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser delegate = super.loadUser(userRequest);
        String email = (String) delegate.getAttributes().get("email");
        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("missing_email"),
                    "Google sign-in did not return an email address");
        }
        String name = (String) delegate.getAttributes().get("name");
        Boolean verified = (Boolean) delegate.getAttributes().get("email_verified");
        String utmSource = intents.peekCurrent().utmSource();
        String subject = delegate.getSubject();
        provisioner.provisionFromGoogle(email, name, Boolean.TRUE.equals(verified), utmSource, subject);
        Set<GrantedAuthority> authorities = Set.copyOf(delegate.getAuthorities());
        return new DefaultOidcUser(authorities, delegate.getIdToken(),
                delegate.getUserInfo(), "email");
    }
}
