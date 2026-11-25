package com.emailmessenger.api;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.NewMailEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * docs/API.md §5.24. Not {@code @Transactional}: the stream listens after
 * commit, so the events here are published outside any transaction (the
 * listener's fallback path), and users get unique addresses instead of a
 * rollback.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiEventsIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserService userService;
    @Autowired ApplicationEventPublisher events;

    @Test
    void streamWithoutTokenIs401Envelope() throws Exception {
        mockMvc.perform(get("/api/v1/events").accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("unauthorized"));
    }

    @Test
    void newMailForTheCallerArrivesWithItsConversationKeys() throws Exception {
        String email = "events-" + UUID.randomUUID() + "@example.com";
        User user = userService.register(email, "correct-horse", "Ada Lovelace");
        String token = login(email, "correct-horse");

        MvcResult result = mockMvc.perform(get("/api/v1/events")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();

        events.publishEvent(new NewMailEvent(user.getId(), 2, List.of("aaa", "bbb")));
        events.publishEvent(new NewMailEvent(user.getId() + 100_000, 1, List.of("someone-else")));

        assertThat(result.getResponse().getContentType()).startsWith(MediaType.TEXT_EVENT_STREAM_VALUE);
        assertThat(result.getResponse().getContentAsString())
                .isEqualTo("event:new-mail\ndata:{\"imported\":2,\"conversationKeys\":[\"aaa\",\"bbb\"]}\n\n");
    }

    @Test
    void revokedTokenCannotOpenTheStream() throws Exception {
        String email = "events-" + UUID.randomUUID() + "@example.com";
        userService.register(email, "correct-horse", "Ada Lovelace");
        String token = login(email, "correct-horse");
        mockMvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/events")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isUnauthorized());
    }

    private String login(String email, String password) throws Exception {
        String json = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApiDtos.LoginRequest(email, password, null))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("token").asText();
    }
}
