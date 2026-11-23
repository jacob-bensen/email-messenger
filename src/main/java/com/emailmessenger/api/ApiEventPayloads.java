package com.emailmessenger.api;

import com.emailmessenger.repository.UserRepository;
import com.emailmessenger.web.ConversationListService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Builds the §5.21 event data from the same list service as §5.5 and the website. */
@Service
class ApiEventPayloads {

    static final int MAX_ROWS = 50;

    private final UserRepository users;
    private final ConversationListService conversations;

    ApiEventPayloads(UserRepository users, ConversationListService conversations) {
        this.users = users;
        this.conversations = conversations;
    }

    @Transactional(readOnly = true)
    Optional<ApiDtos.ConversationsEvent> conversationsChanged(long userId, int imported, Set<String> keys) {
        return users.findById(userId).map(owner -> {
            boolean truncated = keys.size() > MAX_ROWS;
            List<ApiDtos.ConversationRow> rows = truncated ? List.of()
                    : conversations.rowsFor(owner, keys).stream().map(ApiDtos.ConversationRow::of).toList();
            return new ApiDtos.ConversationsEvent(imported, rows, truncated,
                    ApiDtos.CountsResponse.of(conversations.counts(owner, null)));
        });
    }
}
