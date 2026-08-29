package com.emailmessenger.web;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Adds {@code siteBaseUrl} + {@code supportEmail} to every model so
 * Thymeleaf templates can build absolute canonical / og:url /
 * twitter:url values and a "Contact support" mailto from any page
 * without each controller re-injecting them. Also exposes
 * {@code oauthGoogleEnabled} so the login / register templates can
 * conditionally render the "Continue with Google" button only when the
 * Google client credentials are configured on the deploy.
 */
@ControllerAdvice
class SiteModelAdvice {

    private final SiteProperties siteProperties;
    private final ObjectProvider<ClientRegistrationRepository> oauthRegistrations;

    SiteModelAdvice(SiteProperties siteProperties,
                    ObjectProvider<ClientRegistrationRepository> oauthRegistrations) {
        this.siteProperties = siteProperties;
        this.oauthRegistrations = oauthRegistrations;
    }

    @ModelAttribute("siteBaseUrl")
    String siteBaseUrl() {
        return siteProperties.getBaseUrl();
    }

    @ModelAttribute("supportEmail")
    String supportEmail() {
        return siteProperties.getSupportEmail();
    }

    @ModelAttribute("oauthGoogleEnabled")
    boolean oauthGoogleEnabled() {
        ClientRegistrationRepository repo = oauthRegistrations.getIfAvailable();
        if (repo == null) {
            return false;
        }
        ClientRegistration google = findByRegistrationId(repo, "google");
        return google != null
                && google.getClientId() != null
                && !google.getClientId().isBlank();
    }

    private static ClientRegistration findByRegistrationId(ClientRegistrationRepository repo, String id) {
        if (repo instanceof InMemoryClientRegistrationRepository memory) {
            for (ClientRegistration cr : memory) {
                if (id.equals(cr.getRegistrationId())) {
                    return cr;
                }
            }
            return null;
        }
        return repo.findByRegistrationId(id);
    }
}
