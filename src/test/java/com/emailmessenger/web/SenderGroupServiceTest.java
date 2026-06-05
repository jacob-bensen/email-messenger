package com.emailmessenger.web;

import com.emailmessenger.domain.User;
import com.emailmessenger.repository.EmailThreadRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SenderGroupServiceTest {

    @Mock EmailThreadRepository threads;

    private final User user = new User("u@example.com", "h", "U");

    @Test
    void mapsRowsToSenderGroupWithLabelFallingBackToEmailWhenDisplayNameMissing() {
        when(threads.topSenders(eq(user), any(Pageable.class))).thenReturn(List.of(
                row("ada@acme.com", "Ada Lovelace", 5L),
                row("noname@x.com", null, 2L),
                row("blank@x.com", "  ", 1L)
        ));

        SenderGroupService svc = new SenderGroupService(threads);
        List<SenderGroupService.SenderGroup> groups = svc.topSenders(user);

        assertThat(groups).hasSize(3);
        assertThat(groups.get(0).email()).isEqualTo("ada@acme.com");
        assertThat(groups.get(0).label()).isEqualTo("Ada Lovelace");
        assertThat(groups.get(0).initials()).isEqualTo("AL");
        assertThat(groups.get(0).threadCount()).isEqualTo(5L);
        assertThat(groups.get(1).label()).isEqualTo("noname@x.com");
        assertThat(groups.get(1).initials()).isEqualTo("N");
        assertThat(groups.get(2).label()).isEqualTo("blank@x.com");
        assertThat(groups.get(2).initials()).isEqualTo("B");
    }

    @Test
    void usesDefaultLimitWhenNotSpecified() {
        when(threads.topSenders(eq(user), any(Pageable.class))).thenReturn(List.of());
        SenderGroupService svc = new SenderGroupService(threads);

        svc.topSenders(user);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(threads).topSenders(eq(user), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(SenderGroupService.DEFAULT_LIMIT);
    }

    @Test
    void honorsExplicitLimit() {
        when(threads.topSenders(eq(user), any(Pageable.class))).thenReturn(List.of());
        SenderGroupService svc = new SenderGroupService(threads);

        svc.topSenders(user, 3);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(threads).topSenders(eq(user), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(3);
    }

    @Test
    void emptyResultMapsToEmptyList() {
        when(threads.topSenders(eq(user), any(Pageable.class))).thenReturn(List.of());
        SenderGroupService svc = new SenderGroupService(threads);

        assertThat(svc.topSenders(user)).isEmpty();
    }

    private static EmailThreadRepository.SenderGroupRow row(String email, String displayName, long count) {
        return new EmailThreadRepository.SenderGroupRow() {
            @Override public String getEmail() { return email; }
            @Override public String getDisplayName() { return displayName; }
            @Override public long getThreadCount() { return count; }
        };
    }
}
