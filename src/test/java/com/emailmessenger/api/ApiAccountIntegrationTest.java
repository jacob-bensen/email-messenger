package com.emailmessenger.api;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.billing.BillingPeriod;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.Subscription;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.ApiTokenRepository;
import com.emailmessenger.repository.SubscriptionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** docs/API.md §5.15–5.18. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiAccountIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserService userService;
    @Autowired SubscriptionRepository subscriptions;
    @Autowired ApiTokenRepository apiTokens;
    @PersistenceContext EntityManager em;

    private User owner;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        owner = userService.register("ada@example.com", "correct-horse", "Ada Lovelace");
        token = login("ada@example.com", "Pixel 9");
    }

    @Test
    void accountOnFreeHasUserAndNoBilling() throws Exception {
        authed(get("/api/v1/account"), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("ada@example.com"))
                .andExpect(jsonPath("$.user.plan").value("free"))
                .andExpect(jsonPath("$.user.limits.mailboxes").value(3))
                .andExpect(jsonPath("$.billing").value(nullValue()));
    }

    @Test
    void accountShowsTheWebsitesBillingSummary() throws Exception {
        Subscription sub = new Subscription(owner, "cus_trial", "trialing");
        sub.setPlan(Plan.PRO);
        sub.setBillingPeriod(BillingPeriod.ANNUAL);
        sub.setTrialEndsAt(LocalDateTime.of(2027, 6, 7, 12, 0));
        subscriptions.save(sub);

        authed(get("/api/v1/account"), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billing.label").value("Pro · Annual trial"))
                .andExpect(jsonPath("$.billing.renewsOn").value("2027-06-07"))
                .andExpect(jsonPath("$.billing.trialing").value(true))
                .andExpect(jsonPath("$.billing.canceled").value(false));
    }

    @Test
    void accountRequiresAToken() throws Exception {
        mockMvc.perform(get("/api/v1/account"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("unauthorized"));
    }

    @Test
    void devicesListsOwnTokensWithTheCurrentOneMarked() throws Exception {
        login("ada@example.com", "iPad");
        userService.register("bob@example.com", "correct-horse", "Bob");
        login("bob@example.com", "Bob's phone");

        JsonNode items = json(authed(get("/api/v1/account/devices"), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2))))
                .get("items");
        assertThat(items).extracting(n -> n.get("deviceName").asText()).containsExactlyInAnyOrder("Pixel 9", "iPad");
        JsonNode current = items.get(0).get("current").asBoolean() ? items.get(0) : items.get(1);
        assertThat(current.get("deviceName").asText()).isEqualTo("Pixel 9");
        assertThat(current.get("signedInAt").asText()).endsWith("Z");
        assertThat(current.get("expiresAt").asText()).endsWith("Z");
        assertThat(items.get(0).get("current").asBoolean()).isNotEqualTo(items.get(1).get("current").asBoolean());
    }

    @Test
    void expiredTokensAreNotListed() throws Exception {
        login("ada@example.com", "Old phone");
        em.createQuery("UPDATE ApiToken t SET t.expiresAt = :past WHERE t.deviceName = 'Old phone'")
                .setParameter("past", LocalDateTime.now().minusDays(1))
                .executeUpdate();

        authed(get("/api/v1/account/devices"), token)
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].deviceName").value("Pixel 9"));
    }

    @Test
    void revokingAnotherDeviceSignsItOut() throws Exception {
        String other = login("ada@example.com", "iPad");
        long otherId = deviceId("iPad");

        authed(delete("/api/v1/account/devices/" + otherId), token).andExpect(status().isNoContent());

        authed(get("/api/v1/me"), other).andExpect(status().isUnauthorized());
        authed(get("/api/v1/me"), token).andExpect(status().isOk());
        authed(delete("/api/v1/account/devices/" + otherId), token)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("not_found"));
    }

    @Test
    void revokingTheCurrentDeviceIsALogout() throws Exception {
        authed(delete("/api/v1/account/devices/" + deviceId("Pixel 9")), token).andExpect(status().isNoContent());

        authed(get("/api/v1/me"), token).andExpect(status().isUnauthorized());
    }

    @Test
    void anotherUsersDeviceIsNotFound() throws Exception {
        userService.register("bob@example.com", "correct-horse", "Bob");
        String bobToken = login("bob@example.com", "Bob's phone");
        long bobDevice = deviceId("Bob's phone");

        authed(delete("/api/v1/account/devices/" + bobDevice), token)
                .andExpect(status().isNotFound());
        authed(get("/api/v1/me"), bobToken).andExpect(status().isOk());
    }

    @Test
    void signOutOthersKeepsOnlyTheCaller() throws Exception {
        String ipad = login("ada@example.com", "iPad");
        String laptop = login("ada@example.com", null);
        userService.register("bob@example.com", "correct-horse", "Bob");
        String bobToken = login("bob@example.com", "Bob's phone");

        authed(post("/api/v1/account/devices/sign-out-others"), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revoked").value(2));

        authed(get("/api/v1/me"), ipad).andExpect(status().isUnauthorized());
        authed(get("/api/v1/me"), laptop).andExpect(status().isUnauthorized());
        authed(get("/api/v1/me"), token).andExpect(status().isOk());
        authed(get("/api/v1/me"), bobToken).andExpect(status().isOk());
        authed(post("/api/v1/account/devices/sign-out-others"), token)
                .andExpect(jsonPath("$.revoked").value(0));
    }

    private long deviceId(String deviceName) {
        return apiTokens.findAll().stream()
                .filter(t -> deviceName.equals(t.getDeviceName()))
                .findFirst().orElseThrow().getId();
    }

    private JsonNode json(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private ResultActions authed(MockHttpServletRequestBuilder request, String bearer) throws Exception {
        return mockMvc.perform(request.header("Authorization", "Bearer " + bearer));
    }

    private String login(String email, String deviceName) throws Exception {
        String json = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ApiDtos.LoginRequest(email, "correct-horse", deviceName))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("token").asText();
    }
}
