package com.emailmessenger.api;

import com.emailmessenger.auth.GoogleOAuthProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * First public JSON endpoint intended for the mobile client. Returns
 * cheap, unauthenticated server discovery data so the app can decide
 * whether it can talk to this server before asking the user to sign in.
 * See docs/API.md for the wire format contract.
 */
@RestController
@RequestMapping("/api/v1")
class MetaController {

    private static final String API_VERSION = "1";
    private static final int MIN_MOBILE_BUILD = 1;
    private static final String UNKNOWN_VERSION = "0.0.0-SNAPSHOT";

    private final Clock clock;
    private final GoogleOAuthProperties googleOAuthProperties;
    private final ObjectProvider<BuildProperties> buildProperties;

    MetaController(Clock clock,
                   GoogleOAuthProperties googleOAuthProperties,
                   ObjectProvider<BuildProperties> buildProperties) {
        this.clock = clock;
        this.googleOAuthProperties = googleOAuthProperties;
        this.buildProperties = buildProperties;
    }

    @GetMapping("/meta")
    MetaResponse meta() {
        BuildProperties props = buildProperties.getIfAvailable();
        String version = props == null ? UNKNOWN_VERSION : props.getVersion();
        Features features = new Features(googleOAuthProperties.isEnabled());
        return new MetaResponse(
                API_VERSION,
                version,
                MIN_MOBILE_BUILD,
                Instant.now(clock).toString(),
                List.of("FREE", "PRO", "BUSINESS"),
                features);
    }

    record MetaResponse(
            String apiVersion,
            String serverVersion,
            int minMobileBuild,
            String serverTime,
            List<String> plans,
            Features features) {}

    record Features(boolean googleOAuth) {}
}
