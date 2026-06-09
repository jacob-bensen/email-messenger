package com.emailmessenger.auth;

import com.emailmessenger.billing.BillingService;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.service.ReplyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * With Google client credentials configured, the "Continue with Google"
 * button renders on /login and /register, the OAuth2 client registration
 * is loaded, and Spring Security's authorization-request endpoint
 * redirects to Google's consent page.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "auth.google.client-id=test-client-id.apps.googleusercontent.com",
        "auth.google.client-secret=test-client-secret"
})
class GoogleOAuthSignInIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ClientRegistrationRepository registrationRepository;

    @MockBean ReplyService replyService;
    @MockBean EmailThreadRepository threadRepository;
    @MockBean BillingService billingService;

    @Test
    void clientRegistrationRepositoryHoldsGoogle() {
        ClientRegistration google = registrationRepository.findByRegistrationId("google");
        assertThat(google).isNotNull();
        assertThat(google.getClientId()).isEqualTo("test-client-id.apps.googleusercontent.com");
        assertThat(google.getScopes()).contains("openid", "email", "profile");
    }

    @Test
    void loginPageRendersContinueWithGoogleButton() throws Exception {
        String body = mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(body).contains("Continue with Google");
        assertThat(body).contains("/oauth2/authorization/google");
        assertThat(body).contains("class=\"btn-oauth btn-oauth-google\"");
        assertThat(body).contains("class=\"auth-divider\"");
    }

    @Test
    void registerPageRendersContinueWithGoogleButton() throws Exception {
        String body = mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(body).contains("Continue with Google");
        assertThat(body).contains("/oauth2/authorization/google");
    }

    @Test
    void authorizationRequestEndpointRedirectsToGoogleConsent() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.startsWith(
                                "https://accounts.google.com/o/oauth2/v2/auth")))
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.containsString(
                                "client_id=test-client-id.apps.googleusercontent.com")))
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.containsString("scope=openid")));
    }
}
