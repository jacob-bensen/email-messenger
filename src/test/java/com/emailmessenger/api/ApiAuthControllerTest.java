package com.emailmessenger.api;

import com.emailmessenger.auth.AccountService;
import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.AuthEventType;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.ApiTokenRepository;
import com.emailmessenger.repository.AuthEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The bearer-token login round-trip against the real security chains. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiAuthControllerTest {

    private static final String EMAIL = "api-user@example.com";
    private static final String PASSWORD = "password1";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper json;
    @Autowired UserService userService;
    @Autowired AccountService accountService;
    @Autowired ApiTokenRepository apiTokens;
    @Autowired AuthEventRepository authEvents;

    private User user;

    @BeforeEach
    void setUp() {
        user = userService.register(EMAIL, PASSWORD, "Api User");
    }

    @Test
    void loginIssuesATokenThatAuthenticatesMe() throws Exception {
        JsonNode login = login(EMAIL, PASSWORD);

        String token = login.get("token").asText();
        assertThat(token).startsWith("cxm_");
        assertThat(login.get("expiresAt").asText()).endsWith("Z");
        assertThat(login.get("user").get("email").asText()).isEqualTo(EMAIL);
        assertThat(login.get("user").get("plan").asText()).isEqualTo("free");

        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.displayName").value("Api User"))
                .andExpect(jsonPath("$.emailVerified").value(false))
                .andExpect(jsonPath("$.plan").value("free"));
    }

    @Test
    void onlyTheHashOfTheTokenIsStored() throws Exception {
        String token = login(EMAIL, PASSWORD).get("token").asText();

        assertThat(apiTokens.findAll()).singleElement().satisfies(stored -> {
            assertThat(stored.getTokenHash()).hasSize(64).isNotEqualTo(token);
            assertThat(stored.getDeviceName()).isEqualTo("Pixel 9");
        });
    }

    @Test
    void loginIsCaseInsensitiveOnEmailAndAudited() throws Exception {
        login("API-User@Example.com", PASSWORD);

        assertThat(authEvents.findAll())
                .anyMatch(e -> e.getEventType() == AuthEventType.LOGIN_SUCCESS && EMAIL.equals(e.getEmail()));
    }

    @Test
    void wrongPasswordIs401WithTheErrorEnvelope() throws Exception {
        mockMvc.perform(loginRequest(EMAIL, "nope-nope"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("invalid_credentials"));
    }

    @Test
    void unknownEmailIsIndistinguishableFromWrongPassword() throws Exception {
        mockMvc.perform(loginRequest("nobody@example.com", PASSWORD))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("invalid_credentials"));
    }

    @Test
    void repeatedFailuresLockTheAccountEvenForTheRightPassword() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(loginRequest(EMAIL, "wrong-" + i)).andExpect(status().isUnauthorized());
        }

        mockMvc.perform(loginRequest(EMAIL, PASSWORD))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("too_many_attempts"));
    }

    @Test
    void blankFieldsAre400() throws Exception {
        mockMvc.perform(loginRequest("", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("validation_failed"));
    }

    @Test
    void malformedJsonIs400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{nope"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("validation_failed"));
    }

    @Test
    void missingOrGarbageTokenIsAJson401NotALoginRedirect() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("unauthorized"));

        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer cxm_not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesOnlyThePresentedToken() throws Exception {
        String phone = login(EMAIL, PASSWORD).get("token").asText();
        String tablet = login(EMAIL, PASSWORD).get("token").asText();

        mockMvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + phone))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + phone))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + tablet))
                .andExpect(status().isOk());
    }

    @Test
    void changingThePasswordRevokesEveryToken() throws Exception {
        String token = login(EMAIL, PASSWORD).get("token").asText();

        assertThat(accountService.changePassword(user, PASSWORD, "new-password-1"))
                .isEqualTo(AccountService.PasswordChangeOutcome.OK);

        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aBearerTokenDoesNotOpenTheBrowserSite() throws Exception {
        String token = login(EMAIL, PASSWORD).get("token").asText();

        mockMvc.perform(get("/chats").header("Authorization", "Bearer " + token))
                .andExpect(status().is3xxRedirection());
    }

    private JsonNode login(String email, String password) throws Exception {
        String body = mockMvc.perform(loginRequest(email, password))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder loginRequest(
            String email, String password) throws Exception {
        return post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(
                        Map.of("email", email, "password", password, "deviceName", "Pixel 9")));
    }
}
