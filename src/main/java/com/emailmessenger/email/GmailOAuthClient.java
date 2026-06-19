package com.emailmessenger.email;

import com.emailmessenger.auth.GoogleOAuthProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Drives the Gmail mailbox-connect OAuth flow and mints access tokens for
 * IMAP XOAUTH2. Reuses the same Google client credentials as
 * "Continue with Google" sign-in ({@link GoogleOAuthProperties}); the only
 * difference is the requested scope ({@value #GMAIL_SCOPE}) and that we ask
 * for offline access so Google returns a refresh token we can store and
 * exchange from the background poller.
 */
@Component
public class GmailOAuthClient {

    static final String AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    static final String USERINFO_ENDPOINT = "https://www.googleapis.com/oauth2/v3/userinfo";

    /** Full IMAP/SMTP access — required to log in over IMAP with XOAUTH2. */
    static final String GMAIL_SCOPE = "https://mail.google.com/";
    /** So we can resolve which address was authorized via the userinfo endpoint. */
    static final String EMAIL_SCOPE = "https://www.googleapis.com/auth/userinfo.email";

    private final GoogleOAuthProperties props;
    private final RestClient restClient;

    GmailOAuthClient(GoogleOAuthProperties props, RestClient.Builder restClientBuilder) {
        this.props = props;
        this.restClient = restClientBuilder.build();
    }

    /** The address authorized + the tokens to talk to it. {@code refreshToken} may be null. */
    public record TokenResult(String accessToken, String refreshToken) {}

    /**
     * Google's consent-screen URL. {@code prompt=consent} + {@code access_type=offline}
     * force a refresh token even on a re-authorization, so a reconnect always
     * yields a token we can persist.
     */
    public String buildAuthorizationUrl(String redirectUri, String state) {
        return UriComponentsBuilder.fromUriString(AUTH_ENDPOINT)
                .queryParam("client_id", props.getClientId())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", GMAIL_SCOPE + " " + EMAIL_SCOPE)
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .queryParam("include_granted_scopes", "true")
                .queryParam("state", state)
                .build()
                .encode()
                .toUriString();
    }

    /** Exchanges an authorization code for an access + refresh token. */
    public TokenResult exchangeCode(String code, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", props.getClientId());
        form.add("client_secret", props.getClientSecret());
        form.add("redirect_uri", redirectUri);
        form.add("grant_type", "authorization_code");
        TokenResponse body = postToken(form, "exchange authorization code");
        return new TokenResult(body.access_token(), body.refresh_token());
    }

    /** Mints a fresh access token from a stored refresh token. */
    public String refreshAccessToken(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("refresh_token", refreshToken);
        form.add("client_id", props.getClientId());
        form.add("client_secret", props.getClientSecret());
        form.add("grant_type", "refresh_token");
        TokenResponse body = postToken(form, "refresh access token");
        if (body.access_token() == null || body.access_token().isBlank()) {
            throw new GmailOAuthException("Google returned no access token on refresh", null);
        }
        return body.access_token();
    }

    /** Resolves the email address an access token is scoped to. */
    public String fetchEmail(String accessToken) {
        try {
            UserInfo info = restClient.get()
                    .uri(USERINFO_ENDPOINT)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(UserInfo.class);
            if (info == null || info.email() == null || info.email().isBlank()) {
                throw new GmailOAuthException("Google userinfo returned no email address", null);
            }
            return info.email();
        } catch (RestClientException e) {
            throw new GmailOAuthException("Could not read Google account email: " + e.getMessage(), e);
        }
    }

    private TokenResponse postToken(MultiValueMap<String, String> form, String what) {
        try {
            TokenResponse body = restClient.post()
                    .uri(TOKEN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);
            if (body == null) {
                throw new GmailOAuthException("Empty token response from Google (" + what + ")", null);
            }
            return body;
        } catch (RestClientException e) {
            throw new GmailOAuthException("Failed to " + what + " with Google: " + e.getMessage(), e);
        }
    }

    // Field names mirror Google's snake_case JSON so Jackson binds them directly.
    record TokenResponse(String access_token, String refresh_token,
                         Integer expires_in, String scope, String token_type, String id_token) {}

    record UserInfo(String email, Boolean email_verified, String sub) {}
}
