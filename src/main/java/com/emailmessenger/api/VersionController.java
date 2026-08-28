package com.emailmessenger.api;

import org.springframework.boot.info.BuildProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;

/**
 * {@code GET /api/version} — the first mobile-facing REST endpoint.
 *
 * <p>Returns app slug, running jar version, contract version, and the
 * server clock. The Flutter client (see {@code
 * conexus-mail-mobile/lib/data/api_client.dart}) pings this on launch
 * to prove reachability and to refuse to run against a server whose
 * contract version differs.
 *
 * <p>Contract: {@code email-messenger/docs/API.md#get-apiversion}.
 * Every field here MUST match that document — change both together.
 *
 * <p>Public: whitelisted in {@link com.emailmessenger.auth.SecurityConfig}.
 */
@RestController
@RequestMapping("/api")
class VersionController {

    /**
     * The REST contract version this build implements. Bump only on a
     * breaking change and update {@code docs/API.md#versioning} in the
     * same commit; every mobile client comparing against
     * {@code ApiClient.supportedApi} will refuse the mismatch.
     */
    static final int API_CONTRACT_VERSION = 1;

    private final String appName;
    private final String version;
    private final Clock clock;

    VersionController(ObjectProvider<BuildProperties> buildProperties, Clock clock) {
        BuildProperties props = buildProperties.getIfAvailable();
        // BuildProperties comes from META-INF/build-info.properties, generated
        // by the spring-boot-maven-plugin's build-info goal. It's missing when
        // the app is run from a raw classpath without the build step (some
        // tests); fall back to a stable placeholder so the endpoint stays honest.
        this.appName = "conexus-mail";
        this.version = props != null ? props.getVersion() : "unknown";
        this.clock = clock;
    }

    @GetMapping("/version")
    VersionResponse version() {
        return new VersionResponse(appName, version, API_CONTRACT_VERSION,
                clock.instant().toString());
    }
}
