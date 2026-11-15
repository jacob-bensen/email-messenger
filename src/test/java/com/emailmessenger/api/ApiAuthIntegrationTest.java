package com.emailmessenger.api;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.AuthEventType;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.ApiTokenRepository;
import com.emailmessenger.repository.AuthEventRepository;
import com.emailmessenger.repository.UserRepository;
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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiAuthIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserService userService;
    @Autowired UserRepository users;
    @Autowired ApiTokenRepository tokens;
    @Autowired AuthEventRepository authEvents;
    @Autowired ObjectMapper objectMapper;

    private User ada;

    @BeforeEach
    void createUser() {
        ada = userService.register("ada@example.com", "correct-horse", "Ada Lovelace");
    }

    private String loginBody(String email, String password) throws Exception {
        return objectMapper.writeValueAsString(new ApiDtos.LoginRequest(email, password, "Pixel 9"));
    }

    private String login() throws Exception {
        String json = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("ada@example.com", "correct-horse")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("token").asText();
    }

    @Test
    void loginReturnsTokenAndProfileWithoutCsrfOrSession() throws Exception {
        String json = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(" ADA@example.com ", "correct-horse")))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("ada@example.com"))
                .andExpect(jsonPath("$.user.displayName").value("Ada Lovelace"))
                .andExpect(jsonPath("$.user.plan").value("free"))
                .andExpect(jsonPath("$.user.limits.mailboxes").value(3))
                .andExpect(jsonPath("$.user.limits.threads").value(500))
                .andExpect(jsonPath("$.user.limits.savedSearches").isEmpty())
                .andReturn().getResponse().getContentAsString();

        JsonNode body = objectMapper.readTree(json);
        String token = body.get("token").asText();
        assertThat(token).hasSizeGreaterThanOrEqualTo(43);
        assertThat(body.get("expiresAt").asText()).endsWith("Z");

        var stored = tokens.findWithUserByTokenHash(ApiTokenService.hash(token)).orElseThrow();
        assertThat(stored.getDeviceName()).isEqualTo("Pixel 9");
        assertThat(tokens.findAll()).noneMatch(t -> t.getTokenHash().equals(token));
    }

    @Test
    void loginGoesThroughTheWebsiteAuditTrail() throws Exception {
        login();

        assertThat(users.findById(ada.getId()).orElseThrow().getLastLoginAt()).isNotNull();
        assertThat(authEvents.countRecentForEmail(AuthEventType.LOGIN_SUCCESS, "ada@example.com",
                LocalDateTime.now().minusMinutes(5))).isEqualTo(1);
    }

    @Test
    void wrongPasswordIs401WithEnvelopeAndIsRecorded() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("ada@example.com", "nope")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("invalid_credentials"))
                .andExpect(jsonPath("$.error.message").isNotEmpty());

        assertThat(authEvents.countRecentForEmail(AuthEventType.LOGIN_FAILURE, "ada@example.com",
                LocalDateTime.now().minusMinutes(5))).isEqualTo(1);
    }

    @Test
    void unknownEmailIsTheSame401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("nobody@example.com", "whatever")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("invalid_credentials"));
    }

    @Test
    void repeatedFailuresLockTheAccountWith429() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody("ada@example.com", "nope")));
        }
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("ada@example.com", "correct-horse")))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("too_many_attempts"));
    }

    @Test
    void missingFieldsAre400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ada@example.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("validation_failed"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("validation_failed"));
    }

    @Test
    void meRequiresAValidBearerToken() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("unauthorized"));

        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer not-a-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("unauthorized"));
    }

    @Test
    void meReturnsTheSignedInUser() throws Exception {
        String token = login();

        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ada.getId()))
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.emailVerified").value(false))
                .andExpect(jsonPath("$.plan").value("free"));
    }

    @Test
    void logoutRevokesOnlyThatToken() throws Exception {
        String phone = login();
        String tablet = login();

        mockMvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + phone))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + phone))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + tablet))
                .andExpect(status().isOk());
    }

    @Test
    void disabledUsersTokenStopsWorking() throws Exception {
        String token = login();
        User reloaded = users.findById(ada.getId()).orElseThrow();
        reloaded.setEnabled(false);
        users.saveAndFlush(reloaded);

        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void websiteChainIsUnaffected() throws Exception {
        mockMvc.perform(get("/chats"))
                .andExpect(status().is3xxRedirection());
    }
}
