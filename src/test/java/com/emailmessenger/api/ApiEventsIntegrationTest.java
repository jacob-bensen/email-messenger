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

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
// Not @Transactional: the listener is AFTER_COMMIT, so events are published
// outside a transaction and delivered at once. Addresses are unique per test.
class ApiEventsIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserService userService;
    @Autowired ApplicationEventPublisher events;

    @Test
    void streamsNewMailWithTheTouchedConversationKeys() throws Exception {
        User ada = userService.register("events-ada@example.com", "correct-horse", "Ada Lovelace");
        String token = login("events-ada@example.com");

        MvcResult result = mockMvc.perform(get("/api/v1/events")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();

        events.publishEvent(new NewMailEvent(ada.getId(), 2, Set.of("k2", "k1")));

        assertThat(result.getResponse().getContentType()).startsWith(MediaType.TEXT_EVENT_STREAM_VALUE);
        assertThat(result.getResponse().getContentAsString())
                .contains("event:new-mail")
                .contains("data:{\"imported\":2,\"conversationKeys\":[\"k1\",\"k2\"]}");
    }

    @Test
    void anotherUsersMailIsNotStreamed() throws Exception {
        userService.register("events-ada2@example.com", "correct-horse", "Ada Lovelace");
        User eve = userService.register("events-eve@example.com", "correct-horse", "Eve");
        String token = login("events-ada2@example.com");

        MvcResult result = mockMvc.perform(get("/api/v1/events").header("Authorization", "Bearer " + token))
                .andExpect(request().asyncStarted())
                .andReturn();
        events.publishEvent(new NewMailEvent(eve.getId(), 1, Set.of("k")));

        assertThat(result.getResponse().getContentAsString()).doesNotContain("new-mail");
    }

    @Test
    void requiresABearerToken() throws Exception {
        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("unauthorized"));
    }

    private String login(String email) throws Exception {
        String json = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ApiDtos.LoginRequest(email, "correct-horse", null))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("token").asText();
    }
}
