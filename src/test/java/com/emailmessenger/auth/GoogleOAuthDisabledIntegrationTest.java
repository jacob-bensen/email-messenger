package com.emailmessenger.auth;

import com.emailmessenger.billing.BillingService;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.service.ReplyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Without {@code auth.google.client-id}, no ClientRegistration bean
 * exists, the button is hidden, and {@code /oauth2/authorization/google}
 * doesn't reach Google — the feature is invisible in an unconfigured
 * deploy and crawlers don't see a broken link.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class GoogleOAuthDisabledIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired(required = false) ClientRegistrationRepository registrationRepository;

    @MockBean ReplyService replyService;
    @MockBean EmailThreadRepository threadRepository;
    @MockBean BillingService billingService;

    @Test
    void noClientRegistrationRepositoryBeanIsCreated() {
        assertThat(registrationRepository).isNull();
    }

    @Test
    void loginPageHidesContinueWithGoogleButton() throws Exception {
        String body = mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("Continue with Google");
        assertThat(body).doesNotContain("/oauth2/authorization/google");
    }

    @Test
    void registerPageHidesContinueWithGoogleButton() throws Exception {
        String body = mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("Continue with Google");
        assertThat(body).doesNotContain("/oauth2/authorization/google");
    }

    @Test
    void oauthAuthorizationEndpointIsNotReachable() throws Exception {
        // No registration → Spring Security's OAuth2 authorization filter
        // isn't wired and the path falls through to the catch-all
        // authenticated() matcher, redirecting to /login.
        mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection());
    }
}
