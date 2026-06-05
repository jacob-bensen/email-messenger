package com.emailmessenger.web;

import com.emailmessenger.billing.PlanLimitService;
import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.EmailThreadRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThreadSearchServiceTest {

    @Mock EmailThreadRepository threads;
    @Mock PlanLimitService planLimits;

    private final User user = new User("u@example.com", "h", "U");

    @Test
    void freeUserSearchUsesSubjectOnlyAndChecksBodyOnlyMatch() {
        Page<EmailThread> page = new PageImpl<>(List.of());
        when(planLimits.currentPlan(user)).thenReturn(Plan.FREE);
        when(threads.search(eq(user), eq("invoice"), any())).thenReturn(page);
        when(threads.hasBodyOnlyMatch(user, "invoice")).thenReturn(true);

        ThreadSearchService svc = new ThreadSearchService(threads, planLimits);
        ThreadSearchService.Result result = svc.search(user, "invoice", PageRequest.of(0, 20));

        assertThat(result.page()).isSameAs(page);
        assertThat(result.showBodySearchUpgradeNag()).isTrue();
        verify(threads, never()).searchIncludingBody(any(), anyString(), any());
    }

    @Test
    void freeUserWithNoBodyOnlyMatchDoesNotSurfaceUpgradeNag() {
        Page<EmailThread> page = new PageImpl<>(List.of());
        when(planLimits.currentPlan(user)).thenReturn(Plan.FREE);
        when(threads.search(eq(user), eq("nothing"), any())).thenReturn(page);
        when(threads.hasBodyOnlyMatch(user, "nothing")).thenReturn(false);

        ThreadSearchService svc = new ThreadSearchService(threads, planLimits);
        ThreadSearchService.Result result = svc.search(user, "nothing", PageRequest.of(0, 20));

        assertThat(result.showBodySearchUpgradeNag()).isFalse();
    }

    @Test
    void personalUserSearchUsesBodyInclusivePathAndSkipsNag() {
        Page<EmailThread> page = new PageImpl<>(List.of());
        when(planLimits.currentPlan(user)).thenReturn(Plan.PERSONAL);
        when(threads.searchIncludingBody(eq(user), eq("kickoff"), any())).thenReturn(page);

        ThreadSearchService svc = new ThreadSearchService(threads, planLimits);
        ThreadSearchService.Result result = svc.search(user, "kickoff", PageRequest.of(0, 20));

        assertThat(result.page()).isSameAs(page);
        assertThat(result.showBodySearchUpgradeNag()).isFalse();
        verify(threads, never()).search(any(), anyString(), any());
        verify(threads, never()).hasBodyOnlyMatch(any(), anyString());
    }

    @Test
    void teamAndEnterpriseAlsoGetBodyInclusiveSearch() {
        Page<EmailThread> page = new PageImpl<>(List.of());
        when(threads.searchIncludingBody(eq(user), anyString(), any())).thenReturn(page);

        ThreadSearchService svc = new ThreadSearchService(threads, planLimits);

        when(planLimits.currentPlan(user)).thenReturn(Plan.TEAM);
        assertThat(svc.search(user, "q", PageRequest.of(0, 20)).showBodySearchUpgradeNag()).isFalse();

        when(planLimits.currentPlan(user)).thenReturn(Plan.ENTERPRISE);
        assertThat(svc.search(user, "q", PageRequest.of(0, 20)).showBodySearchUpgradeNag()).isFalse();
    }
}
