package com.emailmessenger.web;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Adds {@code siteBaseUrl} to every model so Thymeleaf templates can
 * build absolute canonical / og:url / twitter:url values from any page
 * without each controller re-injecting it.
 */
@ControllerAdvice
class SiteModelAdvice {

    private final SiteProperties siteProperties;

    SiteModelAdvice(SiteProperties siteProperties) {
        this.siteProperties = siteProperties;
    }

    @ModelAttribute("siteBaseUrl")
    String siteBaseUrl() {
        return siteProperties.getBaseUrl();
    }
}
