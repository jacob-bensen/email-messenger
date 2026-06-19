package com.emailmessenger.email;

import com.emailmessenger.auth.GoogleOAuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

class GmailOAuthClientTest {

    private GoogleOAuthProperties props;
    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private GmailOAuthClient client;

    @BeforeEach
    void setUp() {
        props = new GoogleOAuthProperties();
        props.setClientId("client-123");
        props.setClientSecret("secret-456");
        builder = RestClient.builder();
        // bindTo must run before the client builds the RestClient from this builder.
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GmailOAuthClient(props, builder);
    }

    @Test
    void buildAuthorizationUrlRequestsGmailScopeWithOfflineConsent() {
        String url = client.buildAuthorizationUrl("https://app.test/mailboxes/gmail/callback", "state-xyz");

        assertThat(url).startsWith(GmailOAuthClient.AUTH_ENDPOINT);
        assertThat(url).contains("client_id=client-123");
        assertThat(url).contains("access_type=offline");
        assertThat(url).contains("prompt=consent");
        assertThat(url).contains("state=state-xyz");
        // Scope is URL-encoded (mail.google.com + email), space becomes %20.
        assertThat(url).contains("mail.google.com");
    }

    @Test
    void exchangeCodeReturnsAccessAndRefreshTokens() {
        server.expect(requestTo(GmailOAuthClient.TOKEN_ENDPOINT))
                .andExpect(method(POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("grant_type=authorization_code")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("code=auth-code")))
                .andRespond(withSuccess(
                        "{\"access_token\":\"at-1\",\"refresh_token\":\"rt-1\",\"expires_in\":3599}",
                        MediaType.APPLICATION_JSON));

        GmailOAuthClient.TokenResult result =
                client.exchangeCode("auth-code", "https://app.test/mailboxes/gmail/callback");

        assertThat(result.accessToken()).isEqualTo("at-1");
        assertThat(result.refreshToken()).isEqualTo("rt-1");
        server.verify();
    }

    @Test
    void refreshAccessTokenMintsANewAccessToken() {
        server.expect(requestTo(GmailOAuthClient.TOKEN_ENDPOINT))
                .andExpect(method(POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("grant_type=refresh_token")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("refresh_token=stored-rt")))
                .andRespond(withSuccess(
                        "{\"access_token\":\"at-2\",\"expires_in\":3599}", MediaType.APPLICATION_JSON));

        assertThat(client.refreshAccessToken("stored-rt")).isEqualTo("at-2");
        server.verify();
    }

    @Test
    void refreshAccessTokenWrapsHttpErrorsAsGmailOAuthException() {
        server.expect(requestTo(GmailOAuthClient.TOKEN_ENDPOINT))
                .andRespond(withStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
                        .body("{\"error\":\"invalid_grant\"}").contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.refreshAccessToken("dead-rt"))
                .isInstanceOf(GmailOAuthException.class);
    }

    @Test
    void fetchEmailReadsAddressFromUserinfoWithBearerToken() {
        server.expect(requestTo(GmailOAuthClient.USERINFO_ENDPOINT))
                .andExpect(method(GET))
                .andExpect(header("Authorization", "Bearer at-1"))
                .andRespond(withSuccess(
                        "{\"email\":\"user@gmail.com\",\"email_verified\":true,\"sub\":\"42\"}",
                        MediaType.APPLICATION_JSON));

        assertThat(client.fetchEmail("at-1")).isEqualTo("user@gmail.com");
        server.verify();
    }
}
