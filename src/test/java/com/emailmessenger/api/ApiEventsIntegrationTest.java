package com.emailmessenger.api;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.NewMailEvent;
import com.emailmessenger.web.ChatEventStream;
import com.emailmessenger.web.ConversationUpdatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

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
    @Autowired ChatEventStream eventStream;

    private User owner;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        owner = userService.register("ada@example.com", "correct-horse", "Ada Lovelace");
        String json = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ApiDtos.LoginRequest("ada@example.com", "correct-horse", "Pixel 9"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readTree(json).get("token").asText();
    }

    @Test
    void streamDeliversTheUsersNewMailEventsWithConversationKeys() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/events")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();

        eventStream.onNewMail(new NewMailEvent(owner.getId() + 1, 9, List.of("someone-else")));
        eventStream.onNewMail(new NewMailEvent(owner.getId(), 2,
                List.of("154e1e0a1872533ddf397a16e5a65dcf")));

        String body = result.getResponse().getContentAsString();
        assertThat(result.getResponse().getContentType()).startsWith(MediaType.TEXT_EVENT_STREAM_VALUE);
        assertThat(body).contains("event:new-mail")
                .contains("data:{\"imported\":2,\"conversationKeys\":[\"154e1e0a1872533ddf397a16e5a65dcf\"]}")
                .doesNotContain("someone-else");
    }

    @Test
    void streamDeliversConversationUpdatedEvents() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/events")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();

        eventStream.onConversationUpdated(new ConversationUpdatedEvent(owner.getId(),
                ConversationUpdatedEvent.Change.READ, List.of("154e1e0a1872533ddf397a16e5a65dcf")));

        assertThat(result.getResponse().getContentAsString())
                .contains("event:conversation-updated")
                .contains("data:{\"change\":\"read\",\"conversationKeys\":[\"154e1e0a1872533ddf397a16e5a65dcf\"]}");
    }

    @Test
    void streamRequiresAToken() throws Exception {
        mockMvc.perform(get("/api/v1/events").accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/events")
                        .header("Authorization", "Bearer not-a-token")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isUnauthorized());
    }
}
