package com.emailmessenger.digest;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.billing.StripeCheckoutGateway;
import com.emailmessenger.billing.StripePortalGateway;
import com.emailmessenger.domain.DigestEmailPreference;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.DigestEmailPreferenceRepository;
import com.emailmessenger.repository.UserRepository;
import com.emailmessenger.service.ReplyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class DigestControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserService userService;
    @Autowired UserRepository users;
    @Autowired DigestEmailPreferenceRepository preferences;

    @MockBean StripeCheckoutGateway stripeCheckout;
    @MockBean StripePortalGateway stripePortal;
    @MockBean ReplyService replyService;

    private User user;
    private DigestEmailPreference prefs;

    @BeforeEach
    void seed() {
        userService.register("opt@example.com", "password1", "Opt");
        user = users.findByEmail("opt@example.com").orElseThrow();
        prefs = preferences.save(new DigestEmailPreference(user, "tok-deadbeef-1234"));
    }

    @Test
    void validTokenFlipsOptedOutAndShowsConfirmation() throws Exception {
        mockMvc.perform(get("/digest/opt-out").param("token", "tok-deadbeef-1234"))
                .andExpect(status().isOk())
                .andExpect(view().name("digest/opt-out"))
                .andExpect(model().attribute("status", "ok"))
                .andExpect(model().attribute("email", "opt@example.com"));

        DigestEmailPreference after = preferences.findByUser(user).orElseThrow();
        assertThat(after.isOptedOut()).isTrue();
    }

    @Test
    void unknownTokenShowsInvalidPageWithoutSideEffects() throws Exception {
        mockMvc.perform(get("/digest/opt-out").param("token", "not-a-real-token"))
                .andExpect(status().isOk())
                .andExpect(view().name("digest/opt-out"))
                .andExpect(model().attribute("status", "invalid"));

        assertThat(preferences.findByUser(user).orElseThrow().isOptedOut()).isFalse();
    }

    @Test
    void missingTokenShowsInvalidPage() throws Exception {
        mockMvc.perform(get("/digest/opt-out"))
                .andExpect(status().isOk())
                .andExpect(view().name("digest/opt-out"))
                .andExpect(model().attribute("status", "invalid"));
    }

    @Test
    void repeatedClickIsIdempotent() throws Exception {
        mockMvc.perform(get("/digest/opt-out").param("token", "tok-deadbeef-1234"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/digest/opt-out").param("token", "tok-deadbeef-1234"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("status", "ok"));

        assertThat(preferences.findByUser(user).orElseThrow().isOptedOut()).isTrue();
    }
}
