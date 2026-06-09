package com.emailmessenger.auth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

/**
 * Supplies the Google {@link ClientRegistration} only when both
 * {@code auth.google.client-id} and {@code client-secret} are
 * non-blank. Without them, no bean is registered — {@link
 * SecurityConfig}'s {@code ObjectProvider} check then leaves
 * {@code oauth2Login()} disabled so an unconfigured deploy still boots
 * (Spring Boot's own OAuth2 auto-config refuses to build a registration
 * with a blank client-id, which would otherwise crash the app on
 * startup).
 */
@Configuration
@ConditionalOnExpression(
        "!T(org.springframework.util.StringUtils).isEmpty('${auth.google.client-id:}')"
                + " && !T(org.springframework.util.StringUtils).isEmpty('${auth.google.client-secret:}')")
class GoogleOAuthClientRegistrationConfig {

    @Bean
    ClientRegistrationRepository clientRegistrationRepository(GoogleOAuthProperties props) {
        ClientRegistration google = CommonOAuth2Provider.GOOGLE
                .getBuilder("google")
                .clientId(props.getClientId())
                .clientSecret(props.getClientSecret())
                .redirectUri(props.getRedirectUri())
                .build();
        return new InMemoryClientRegistrationRepository(google);
    }
}
