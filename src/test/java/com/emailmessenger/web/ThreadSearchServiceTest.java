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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
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
    private final ThreadFilters none = ThreadFilters.NONE;

    @Test
    void freeUserSearchUsesSubjectOnlyAndChecksBodyOnlyMatch() {
        Page<EmailThread> page = new PageImpl<>(List.of());
        when(planLimits.currentPlan(user)).thenReturn(Plan.FREE);
        when(threads.search(eq(user), eq("invoice"), eq(null), eq(null), eq(false), eq(false), any())).thenReturn(page);
        when(threads.hasBodyOnlyMatch(user, "invoice", null, null, false, false)).thenReturn(true);

        ThreadSearchService svc = new ThreadSearchService(threads, planLimits);
        ThreadSearchService.Result result = svc.search(user, "invoice", null, none, PageRequest.of(0, 20));

        assertThat(result.page()).isSameAs(page);
        assertThat(result.showBodySearchUpgradeNag()).isTrue();
        verify(threads, never()).searchIncludingBody(any(), anyString(), any(), any(), anyBoolean(), anyBoolean(), any());
    }

    @Test
    void freeUserWithNoBodyOnlyMatchDoesNotSurfaceUpgradeNag() {
        Page<EmailThread> page = new PageImpl<>(List.of());
        when(planLimits.currentPlan(user)).thenReturn(Plan.FREE);
        when(threads.search(eq(user), eq("nothing"), eq(null), eq(null), eq(false), eq(false), any())).thenReturn(page);
        when(threads.hasBodyOnlyMatch(user, "nothing", null, null, false, false)).thenReturn(false);

        ThreadSearchService svc = new ThreadSearchService(threads, planLimits);
        ThreadSearchService.Result result = svc.search(user, "nothing", null, none, PageRequest.of(0, 20));

        assertThat(result.showBodySearchUpgradeNag()).isFalse();
    }

    @Test
    void personalUserSearchUsesBodyInclusivePathAndSkipsNag() {
        Page<EmailThread> page = new PageImpl<>(List.of());
        when(planLimits.currentPlan(user)).thenReturn(Plan.PERSONAL);
        when(threads.searchIncludingBody(eq(user), eq("kickoff"), eq(null), eq(null), eq(false), eq(false), any()))
                .thenReturn(page);

        ThreadSearchService svc = new ThreadSearchService(threads, planLimits);
        ThreadSearchService.Result result = svc.search(user, "kickoff", null, none, PageRequest.of(0, 20));

        assertThat(result.page()).isSameAs(page);
        assertThat(result.showBodySearchUpgradeNag()).isFalse();
        verify(threads, never()).search(any(), anyString(), any(), any(), anyBoolean(), anyBoolean(), any());
        verify(threads, never()).hasBodyOnlyMatch(any(), anyString(), any(), any(), anyBoolean(), anyBoolean());
    }

    @Test
    void teamAndEnterpriseAlsoGetBodyInclusiveSearch() {
        Page<EmailThread> page = new PageImpl<>(List.of());
        when(threads.searchIncludingBody(eq(user), anyString(), eq(null), eq(null), eq(false), eq(false), any()))
                .thenReturn(page);

        ThreadSearchService svc = new ThreadSearchService(threads, planLimits);

        when(planLimits.currentPlan(user)).thenReturn(Plan.TEAM);
        assertThat(svc.search(user, "q", null, none, PageRequest.of(0, 20)).showBodySearchUpgradeNag()).isFalse();

        when(planLimits.currentPlan(user)).thenReturn(Plan.ENTERPRISE);
        assertThat(svc.search(user, "q", null, none, PageRequest.of(0, 20)).showBodySearchUpgradeNag()).isFalse();
    }

    @Test
    void senderOnlyFilterRoutesThroughFindByOwnerAndSender() {
        Page<EmailThread> page = new PageImpl<>(List.of());
        when(threads.findByOwnerAndSender(eq(user), eq("ada@acme.com"), eq(null), eq(false), eq(false), any()))
                .thenReturn(page);

        ThreadSearchService svc = new ThreadSearchService(threads, planLimits);
        ThreadSearchService.Result result = svc.search(user, "", "ada@acme.com", none, PageRequest.of(0, 20));

        assertThat(result.page()).isSameAs(page);
        assertThat(result.showBodySearchUpgradeNag()).isFalse();
        verify(threads, never()).search(any(), anyString(), any(), any(), anyBoolean(), anyBoolean(), any());
        verify(threads, never()).searchIncludingBody(any(), anyString(), any(), any(), anyBoolean(), anyBoolean(), any());
        verify(planLimits, never()).currentPlan(any());
    }

    @Test
    void freeUserCombinedQueryAndSenderPropagatesSenderToBothQueries() {
        Page<EmailThread> page = new PageImpl<>(List.of());
        when(planLimits.currentPlan(user)).thenReturn(Plan.FREE);
        when(threads.search(eq(user), eq("planning"), eq("ada@acme.com"), eq(null), eq(false), eq(false), any()))
                .thenReturn(page);
        when(threads.hasBodyOnlyMatch(user, "planning", "ada@acme.com", null, false, false)).thenReturn(true);

        ThreadSearchService svc = new ThreadSearchService(threads, planLimits);
        ThreadSearchService.Result result = svc.search(user, "planning", "ada@acme.com", none,
                PageRequest.of(0, 20));

        assertThat(result.showBodySearchUpgradeNag()).isTrue();
        verify(threads).search(eq(user), eq("planning"), eq("ada@acme.com"), eq(null), eq(false), eq(false), any());
        verify(threads).hasBodyOnlyMatch(user, "planning", "ada@acme.com", null, false, false);
    }

    @Test
    void paidUserCombinedQueryAndSenderHitsBodyInclusiveSearchWithSender() {
        Page<EmailThread> page = new PageImpl<>(List.of());
        when(planLimits.currentPlan(user)).thenReturn(Plan.PERSONAL);
        when(threads.searchIncludingBody(eq(user), eq("invoice"), eq("ada@acme.com"),
                eq(null), eq(false), eq(false), any()))
                .thenReturn(page);

        ThreadSearchService svc = new ThreadSearchService(threads, planLimits);
        ThreadSearchService.Result result = svc.search(user, "invoice", "ada@acme.com", none,
                PageRequest.of(0, 20));

        assertThat(result.page()).isSameAs(page);
        assertThat(result.showBodySearchUpgradeNag()).isFalse();
    }

    @Test
    void blankQueryNoSenderNoFiltersIsAnIllegalRequest() {
        ThreadSearchService svc = new ThreadSearchService(threads, planLimits);

        assertThatThrownBy(() -> svc.search(user, "", null, none, PageRequest.of(0, 20)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blankQueryNoSenderButFilterChipActiveRoutesToFindByOwnerFiltered() {
        Page<EmailThread> page = new PageImpl<>(List.of());
        ThreadFilters f = new ThreadFilters(null, null, true, false);
        when(threads.findByOwnerFiltered(eq(user), eq(null), eq(true), eq(false), any())).thenReturn(page);

        ThreadSearchService svc = new ThreadSearchService(threads, planLimits);
        ThreadSearchService.Result result = svc.search(user, "", null, f, PageRequest.of(0, 20));

        assertThat(result.page()).isSameAs(page);
        verify(threads).findByOwnerFiltered(eq(user), eq(null), eq(true), eq(false), any());
        verify(planLimits, never()).currentPlan(any());
    }

    @Test
    void senderFilterAndChipFiltersIntersectThroughFindByOwnerAndSender() {
        Page<EmailThread> page = new PageImpl<>(List.of());
        LocalDateTime since = LocalDateTime.of(2026, 5, 30, 0, 0);
        ThreadFilters f = new ThreadFilters(since, "7d", false, true);
        when(threads.findByOwnerAndSender(eq(user), eq("ada@acme.com"),
                eq(since), eq(false), eq(true), any())).thenReturn(page);

        ThreadSearchService svc = new ThreadSearchService(threads, planLimits);
        ThreadSearchService.Result result = svc.search(user, "", "ada@acme.com", f, PageRequest.of(0, 20));

        assertThat(result.page()).isSameAs(page);
    }

    @Test
    void freeUserQueryAndChipFiltersAllPropagateToRepoCalls() {
        Page<EmailThread> page = new PageImpl<>(List.of());
        LocalDateTime since = LocalDateTime.of(2026, 5, 30, 0, 0);
        ThreadFilters f = new ThreadFilters(since, "7d", true, true);
        when(planLimits.currentPlan(user)).thenReturn(Plan.FREE);
        when(threads.search(eq(user), eq("invoice"), eq(null),
                eq(since), eq(true), eq(true), any())).thenReturn(page);
        when(threads.hasBodyOnlyMatch(user, "invoice", null, since, true, true)).thenReturn(false);

        ThreadSearchService svc = new ThreadSearchService(threads, planLimits);
        ThreadSearchService.Result result = svc.search(user, "invoice", null, f, PageRequest.of(0, 20));

        assertThat(result.page()).isSameAs(page);
        verify(threads).hasBodyOnlyMatch(user, "invoice", null, since, true, true);
    }
}
