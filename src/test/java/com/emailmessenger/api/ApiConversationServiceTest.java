package com.emailmessenger.api;

import com.emailmessenger.domain.User;
import com.emailmessenger.email.MailAccountService;
import com.emailmessenger.email.MailboxPollingService;
import com.emailmessenger.repository.UserRepository;
import com.emailmessenger.service.ConversationListItem;
import com.emailmessenger.web.ChatSendService;
import com.emailmessenger.web.ChatService;
import com.emailmessenger.web.ConversationListService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiConversationServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final ConversationListService list = mock(ConversationListService.class);
    private final ApiConversationService service = new ApiConversationService(users,
            mock(MailAccountService.class), mock(MailboxPollingService.class), list,
            mock(ChatService.class), mock(ChatSendService.class), mock(ApiSendIdempotency.class));
    private final User owner = mock(User.class);

    @Test
    void newMailUpdateCarriesTheChangedRowsAndUnscopedCounts() {
        when(users.findById(7L)).thenReturn(Optional.of(owner));
        when(list.items(owner, Set.of("k1"))).thenReturn(List.of(item("k1")));
        when(list.counts(owner, null)).thenReturn(new ConversationListService.ChatCounts(4, 2, 1, 0));

        ApiDtos.NewMailUpdate update = service.newMailUpdate(7L, 3, Set.of("k1")).orElseThrow();

        assertThat(update.imported()).isEqualTo(3);
        assertThat(update.conversations()).extracting(ApiDtos.ConversationRow::key).containsExactly("k1");
        assertThat(update.conversations().get(0).lastActivityAt()).hasToString("2026-11-26T09:30:00Z");
        assertThat(update.complete()).isTrue();
        assertThat(update.counts()).isEqualTo(new ApiDtos.CountsResponse(4, 2, 1, 0));
    }

    @Test
    void moreThanFiftyChangedConversationsAreCutAndFlaggedIncomplete() {
        when(users.findById(7L)).thenReturn(Optional.of(owner));
        List<ConversationListItem> rows = IntStream.range(0, 51).mapToObj(i -> item("k" + i)).toList();
        when(list.items(owner, Set.of("x"))).thenReturn(rows);
        when(list.counts(owner, null)).thenReturn(new ConversationListService.ChatCounts(51, 0, 0, 0));

        ApiDtos.NewMailUpdate update = service.newMailUpdate(7L, 51, Set.of("x")).orElseThrow();

        assertThat(update.conversations()).hasSize(50);
        assertThat(update.conversations().get(0).key()).isEqualTo("k0");
        assertThat(update.complete()).isFalse();
    }

    @Test
    void aDeletedUserGetsNoUpdate() {
        when(users.findById(7L)).thenReturn(Optional.empty());

        assertThat(service.newMailUpdate(7L, 1, Set.of("k"))).isEmpty();
    }

    private static ConversationListItem item(String key) {
        return new ConversationListItem(key, "Ada", "A", false, 1, "hi",
                LocalDateTime.of(2026, 11, 26, 9, 30), true, 1, List.of("me@x.com"), "ada@x.com");
    }
}
