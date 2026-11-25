package com.emailmessenger.api;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.repository.ApiTokenRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** docs/API.md §5.24. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiEventsIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserService userService;
    @Autowired ApiEventStream events;
    @Autowired ApiTokenRepository apiTokens;

    private long userId;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        userId = userService.register("ada@example.com", "correct-horse", "Ada Lovelace").getId();
        String json = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ApiDtos.LoginRequest("ada@example.com", "correct-horse", "Pixel 9"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readTree(json).get("token").asText();
    }

    @Test
    void openingTheStreamSubscribesTheDevice() throws Exception {
        mockMvc.perform(get("/api/v1/events")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted());

        assertThat(events.activeCount(userId)).isEqualTo(1);
    }

    @Test
    void withoutATokenItIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/events").accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("unauthorized"));
    }

    @Test
    void activeTokenQueryIgnoresExpiredAndUnknownIds() {
        long tokenId = apiTokens.findActiveByUserId(userId, LocalDateTime.now()).get(0).getId();

        assertThat(apiTokens.findActiveIdsIn(List.of(tokenId, 999_999L), LocalDateTime.now()))
                .containsExactly(tokenId);
        assertThat(apiTokens.findActiveIdsIn(List.of(tokenId), LocalDateTime.now().plusDays(31)))
                .isEmpty();
    }
}
