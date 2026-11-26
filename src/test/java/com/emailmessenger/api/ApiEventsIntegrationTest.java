package com.emailmessenger.api;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.NewMailEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiEventsIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserService userService;
    @Autowired ApplicationEventPublisher events;

    private User owner;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        owner = userService.register("ada@example.com", "correct-horse", "Ada Lovelace");
        String json = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ApiDtos.LoginRequest("ada@example.com", "correct-horse", null))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readTree(json).get("token").asText();
    }

    @Test
    void streamsConversationsChangedWithTheChangedKeys() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/events").header("Authorization", "Bearer " + token))
                .andExpect(request().asyncStarted())
                .andReturn();

        // Off the test's transaction, so the after-commit listener runs at once (as a poll's would after commit).
        CompletableFuture.runAsync(() -> events.publishEvent(new NewMailEvent(owner.getId(), 2, List.of("k1", "k2"))))
                .join();

        assertThat(result.getResponse().getContentAsString())
                .contains("event:conversations-changed\ndata:{\"imported\":2,\"keys\":[\"k1\",\"k2\"]}\n\n")
                .doesNotContain("new-mail");
        assertThat(result.getResponse().getContentType()).startsWith(MediaType.TEXT_EVENT_STREAM_VALUE);
    }

    @Test
    void anotherUsersMailIsNotStreamed() throws Exception {
        User other = userService.register("eve@example.com", "correct-horse", "Eve");
        MvcResult result = mockMvc.perform(get("/api/v1/events").header("Authorization", "Bearer " + token))
                .andExpect(request().asyncStarted())
                .andReturn();

        CompletableFuture.runAsync(() -> events.publishEvent(new NewMailEvent(other.getId(), 1, List.of("k9"))))
                .join();

        assertThat(result.getResponse().getContentAsString()).doesNotContain("k9");
    }

    @Test
    void requiresABearerToken() throws Exception {
        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("unauthorized"));
    }
}
