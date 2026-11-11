package com.emailmessenger.api;

import com.emailmessenger.auth.LoginThrottleService;
import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.AuthEventType;
import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.AuthEventRepository;
import com.emailmessenger.repository.MailAccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthApiControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper json;
    @Autowired UserService userService;
    @Autowired AuthEventRepository events;
    @Autowired MailAccountRepository mailAccounts;
    @Autowired LoginThrottleService throttle;

    private ResultActions login(String email, String password) throws Exception {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("email", email);
        body.put("password", password);
        body.put("deviceName", "Test Phone");
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)));
    }

    private String tokenFor(String email, String password) throws Exception {
        String response = login(email, password).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("token").asText();
    }

    private long count(AuthEventType type, String email) {
        return events.findAll().stream()
                .filter(e -> e.getEventType() == type && email.equals(e.getEmail()))
                .count();
    }

    @Test
    void loginReturnsTokenAndUserWithoutNeedingCsrf() throws Exception {
        User user = userService.register("api-user@example.com", "password1", "Api User");
        MailAccount account = mailAccounts.save(new MailAccount(
                user, "imap.example.com", 993, true, "Work@Example.com", "cipher"));

        login(" API-User@example.com ", "password1")
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token", startsWith("cmx_")))
                .andExpect(jsonPath("$.expiresAt").isString())
                .andExpect(jsonPath("$.user.id").value(user.getId()))
                .andExpect(jsonPath("$.user.email").value("api-user@example.com"))
                .andExpect(jsonPath("$.user.displayName").value("Api User"))
                .andExpect(jsonPath("$.user.emailVerified").value(false))
                .andExpect(jsonPath("$.user.plan").value("FREE"))
                .andExpect(jsonPath("$.user.mailboxes.length()").value(1))
                .andExpect(jsonPath("$.user.mailboxes[0].id").value(account.getId()))
                .andExpect(jsonPath("$.user.mailboxes[0].address").value("work@example.com"));

        assertThat(count(AuthEventType.LOGIN_SUCCESS, "api-user@example.com")).isEqualTo(1);
    }

    @Test
    void tokenAuthenticatesMe() throws Exception {
        userService.register("me@example.com", "password1", null);
        String token = tokenFor("me@example.com", "password1");

        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me@example.com"))
                .andExpect(jsonPath("$.mailboxes.length()").value(0));
    }

    @Test
    void wrongPasswordIs401AndRecordsFailure() throws Exception {
        userService.register("wrong@example.com", "password1", null);

        login("wrong@example.com", "nope-nope")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("invalid_credentials"))
                .andExpect(jsonPath("$.error.message").isString());

        assertThat(count(AuthEventType.LOGIN_FAILURE, "wrong@example.com")).isEqualTo(1);
    }

    @Test
    void unknownEmailLooksLikeWrongPassword() throws Exception {
        login("nobody@example.com", "password1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("invalid_credentials"));
    }

    @Test
    void googleOnlyAccountCannotPasswordLogin() throws Exception {
        User user = userService.register("google@example.com", "password1", null);
        user.setGoogleSubject("sub-123");
        user.setPasswordSet(false);

        login("google@example.com", "password1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("invalid_credentials"));
    }

    @Test
    void repeatedFailuresLockOutEvenTheCorrectPassword() throws Exception {
        userService.register("locked@example.com", "password1", null);
        for (int i = 0; i < throttle.getMaxFailures(); i++) {
            login("locked@example.com", "bad-password").andExpect(status().isUnauthorized());
        }

        login("locked@example.com", "password1")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("too_many_attempts"));

        assertThat(count(AuthEventType.ACCOUNT_LOCKED, "locked@example.com")).isEqualTo(1);
    }

    @Test
    void missingFieldsAre400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@example.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("invalid_request"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("invalid_request"));
    }

    @Test
    void requestsWithoutAValidTokenGetJson401NotALoginRedirect() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("unauthorized"));

        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer cmx_bogus"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("unauthorized"));
    }

    @Test
    void unknownPathOrWrongMethodIsJson404() throws Exception {
        userService.register("lost@example.com", "password1", null);
        String token = tokenFor("lost@example.com", "password1");

        mockMvc.perform(get("/api/v1/nope").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("not_found"));

        mockMvc.perform(get("/api/v1/nope"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void logoutRevokesTheToken() throws Exception {
        userService.register("bye@example.com", "password1", null);
        String token = tokenFor("bye@example.com", "password1");

        mockMvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiTokenDoesNotAuthenticateTheWebsite() throws Exception {
        userService.register("split@example.com", "password1", null);
        String token = tokenFor("split@example.com", "password1");

        mockMvc.perform(get("/chats").header("Authorization", "Bearer " + token))
                .andExpect(status().is3xxRedirection());
    }
}
