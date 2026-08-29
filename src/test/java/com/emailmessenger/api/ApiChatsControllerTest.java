package com.emailmessenger.api;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.UserRepository;
import com.emailmessenger.service.ConversationListItem;
import com.emailmessenger.web.ConversationListService;
import com.emailmessenger.web.ConversationListService.ChatFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiChatsControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserService userService;
    @Autowired UserRepository userRepository;

    @MockitoBean ConversationListService conversationListService;

    private User owner;

    @BeforeEach
    void setUp() {
        userService.register("chat-api@example.com", "password1", "Chat API");
        owner = userRepository.findByEmail("chat-api@example.com").orElseThrow();
    }

    @Test
    void anonymousGets401NotRedirect() throws Exception {
        mockMvc.perform(get("/api/v1/chats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "chat-api@example.com")
    void listsChatsWithDefaultPagination() throws Exception {
        LocalDateTime last = LocalDateTime.of(2026, 8, 29, 13, 22, 4);
        ConversationListItem item = new ConversationListItem(
                "alice@example.com|bob@example.com",
                "Alice, Bob",
                "A",
                true,
                2,
                "You: sounds good",
                last,
                false,
                3L,
                List.of("me@example.com"),
                "alice@example.com");
        Page<ConversationListItem> page = new PageImpl<>(
                List.of(item), PageRequest.of(0, 30), 42L);
        when(conversationListService.list(eq(owner), any(), any(), eq(ChatFilter.ALL), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/chats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(30))
                .andExpect(jsonPath("$.totalElements").value(42))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.content[0].key")
                        .value("alice@example.com|bob@example.com"))
                .andExpect(jsonPath("$.content[0].title").value("Alice, Bob"))
                .andExpect(jsonPath("$.content[0].group").value(true))
                .andExpect(jsonPath("$.content[0].memberCount").value(2))
                .andExpect(jsonPath("$.content[0].preview").value("You: sounds good"))
                .andExpect(jsonPath("$.content[0].threadCount").value(3))
                .andExpect(jsonPath("$.content[0].unread").value(false))
                .andExpect(jsonPath("$.content[0].accounts[0]").value("me@example.com"))
                .andExpect(jsonPath("$.content[0].senderEmail").value("alice@example.com"))
                .andExpect(jsonPath("$.content[0].lastActivityEpochMillis")
                        .value(last.toInstant(ZoneOffset.UTC).toEpochMilli()));
    }

    @Test
    @WithMockUser(username = "chat-api@example.com")
    void passesFilterMailboxAndQueryThrough() throws Exception {
        when(conversationListService.list(eq(owner), eq("me@example.com"), eq("invoice"),
                eq(ChatFilter.UNREAD), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 30), 0L));

        mockMvc.perform(get("/api/v1/chats")
                        .param("filter", "unread")
                        .param("mailbox", "me@example.com")
                        .param("q", "invoice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(conversationListService).list(
                eq(owner), eq("me@example.com"), eq("invoice"),
                eq(ChatFilter.UNREAD), any(Pageable.class));
    }

    @Test
    @WithMockUser(username = "chat-api@example.com")
    void rejectsUnknownFilter() throws Exception {
        mockMvc.perform(get("/api/v1/chats").param("filter", "bogus"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "chat-api@example.com")
    void rejectsSizeAboveMax() throws Exception {
        mockMvc.perform(get("/api/v1/chats").param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "chat-api@example.com")
    void nullAccountsFieldSerializesAsEmptyList() throws Exception {
        ConversationListItem noAccounts = new ConversationListItem(
                "solo",
                "Solo",
                "S",
                false, 1,
                "hi", LocalDateTime.of(2026, 1, 1, 0, 0),
                false, 1L,
                null,
                "solo@example.com");
        when(conversationListService.list(eq(owner), any(), any(), eq(ChatFilter.ALL), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(noAccounts), PageRequest.of(0, 30), 1L));

        mockMvc.perform(get("/api/v1/chats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].accounts").isArray())
                .andExpect(jsonPath("$.content[0].accounts.length()").value(0));

        assertThat(ChatSummary.from(noAccounts).accounts()).isEmpty();
    }
}
