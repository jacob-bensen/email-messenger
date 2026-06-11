package com.emailmessenger.admin;

import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.Team;
import com.emailmessenger.domain.ThreadNote;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.PlanChangeEventRepository;
import com.emailmessenger.repository.SubscriptionRepository;
import com.emailmessenger.repository.ThreadNoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamAdoptionMetricsServiceTest {

    @Mock ThreadNoteRepository notes;
    @Mock PlanChangeEventRepository planChanges;
    @Mock SubscriptionRepository subscriptions;

    private final LocalDateTime now = LocalDateTime.of(2026, 6, 11, 12, 0);
    private final Clock fixedClock = Clock.fixed(now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private TeamAdoptionMetricsService service;

    @BeforeEach
    void setUp() {
        service = new TeamAdoptionMetricsService(notes, planChanges, subscriptions, fixedClock);
    }

    @Test
    void cutoffPassedDownIsThirtyDaysBeforeClockNow() {
        when(notes.countByCreatedAtAfter(any())).thenReturn(0L);
        when(notes.countByCreatedAtBetween(any(), any())).thenReturn(0L);
        when(planChanges.countDistinctUsersByTransitionSince(any(), any(), any())).thenReturn(0L);
        when(planChanges.countDistinctUsersByTransitionBetween(any(), any(), any(), any()))
                .thenReturn(0L);
        when(subscriptions.countEntitledOn(any())).thenReturn(0L);

        service.snapshot();

        ArgumentCaptor<LocalDateTime> noteCutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(notes).countByCreatedAtAfter(noteCutoff.capture());
        assertThat(noteCutoff.getValue()).isEqualTo(now.minusDays(30));

        ArgumentCaptor<LocalDateTime> txnCutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(planChanges).countDistinctUsersByTransitionSince(
                eq(Plan.FREE), eq(Plan.TEAM), txnCutoff.capture());
        assertThat(txnCutoff.getValue()).isEqualTo(now.minusDays(30));
    }

    @Test
    void priorWindowQueriesUseTheTwoToOneTimesThirtyDayBracket() {
        when(notes.countByCreatedAtAfter(any())).thenReturn(0L);
        when(notes.countByCreatedAtBetween(any(), any())).thenReturn(0L);
        when(planChanges.countDistinctUsersByTransitionSince(any(), any(), any())).thenReturn(0L);
        when(planChanges.countDistinctUsersByTransitionBetween(any(), any(), any(), any()))
                .thenReturn(0L);
        when(subscriptions.countEntitledOn(any())).thenReturn(0L);

        service.snapshot();

        ArgumentCaptor<LocalDateTime> priorStart = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> priorEnd = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(notes).countByCreatedAtBetween(priorStart.capture(), priorEnd.capture());
        assertThat(priorStart.getValue()).isEqualTo(now.minusDays(60));
        assertThat(priorEnd.getValue()).isEqualTo(now.minusDays(30));

        ArgumentCaptor<LocalDateTime> txnPriorStart = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> txnPriorEnd = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(planChanges).countDistinctUsersByTransitionBetween(
                eq(Plan.FREE), eq(Plan.TEAM), txnPriorStart.capture(), txnPriorEnd.capture());
        assertThat(txnPriorStart.getValue()).isEqualTo(now.minusDays(60));
        assertThat(txnPriorEnd.getValue()).isEqualTo(now.minusDays(30));
    }

    @Test
    void priorWindowCountsArePropagatedAndDeltaPercentReflectsLift() {
        when(notes.countByCreatedAtAfter(any())).thenReturn(20L);
        when(notes.findCreatedSince(any())).thenReturn(List.of());
        when(notes.countByCreatedAtBetween(any(), any())).thenReturn(10L);
        when(planChanges.countDistinctUsersByTransitionSince(eq(Plan.FREE), eq(Plan.TEAM), any()))
                .thenReturn(8L);
        when(planChanges.countDistinctUsersByTransitionSince(eq(Plan.PERSONAL), eq(Plan.TEAM), any()))
                .thenReturn(2L);
        when(planChanges.countDistinctUsersByTransitionBetween(
                eq(Plan.FREE), eq(Plan.TEAM), any(), any())).thenReturn(4L);
        when(planChanges.countDistinctUsersByTransitionBetween(
                eq(Plan.PERSONAL), eq(Plan.TEAM), any(), any())).thenReturn(2L);
        when(subscriptions.countEntitledOn(any())).thenReturn(0L);

        TeamAdoptionMetrics m = service.snapshot();

        assertThat(m.priorNotesPosted()).isEqualTo(10);
        assertThat(m.priorFreeToTeamConversions()).isEqualTo(4);
        assertThat(m.priorPersonalToTeamConversions()).isEqualTo(2);
        assertThat(m.priorTotalTeamConversions()).isEqualTo(6);

        assertThat(m.notesPostedDeltaPercent()).isEqualTo(100);
        assertThat(m.freeToTeamDeltaPercent()).isEqualTo(100);
        assertThat(m.personalToTeamDeltaPercent()).isEqualTo(0);
        assertThat(m.totalTeamConversionsDeltaPercent()).isEqualTo(67);

        assertThat(m.notesPostedDeltaLabel()).isEqualTo("▲ 100% vs. prior 30 days");
        assertThat(m.freeToTeamDeltaLabel()).isEqualTo("▲ 100% vs. prior 30 days");
        assertThat(m.personalToTeamDeltaLabel()).isEqualTo("flat vs. prior 30 days");
        assertThat(m.totalTeamConversionsDeltaLabel()).isEqualTo("▲ 67% vs. prior 30 days");
    }

    @Test
    void deltaLabelReadsNewWhenPriorWindowWasEmpty() {
        when(notes.countByCreatedAtAfter(any())).thenReturn(5L);
        when(notes.findCreatedSince(any())).thenReturn(List.of());
        when(notes.countByCreatedAtBetween(any(), any())).thenReturn(0L);
        when(planChanges.countDistinctUsersByTransitionSince(any(), any(), any())).thenReturn(0L);
        when(planChanges.countDistinctUsersByTransitionBetween(any(), any(), any(), any()))
                .thenReturn(0L);
        when(subscriptions.countEntitledOn(any())).thenReturn(0L);

        TeamAdoptionMetrics m = service.snapshot();

        assertThat(m.notesPostedDeltaLabel()).isEqualTo("new vs. prior 30 days");
        assertThat(m.notesPostedDeltaPercent()).isZero();
    }

    @Test
    void deltaLabelReadsNoPriorDataWhenBothWindowsAreZero() {
        when(notes.countByCreatedAtAfter(any())).thenReturn(0L);
        when(notes.countByCreatedAtBetween(any(), any())).thenReturn(0L);
        when(planChanges.countDistinctUsersByTransitionSince(any(), any(), any())).thenReturn(0L);
        when(planChanges.countDistinctUsersByTransitionBetween(any(), any(), any(), any()))
                .thenReturn(0L);
        when(subscriptions.countEntitledOn(any())).thenReturn(0L);

        TeamAdoptionMetrics m = service.snapshot();

        assertThat(m.notesPostedDeltaLabel()).isEqualTo("no prior-window data");
        assertThat(m.totalTeamConversionsDeltaLabel()).isEqualTo("no prior-window data");
    }

    @Test
    void deltaLabelReadsDownWhenCurrentWindowIsBelowPrior() {
        when(notes.countByCreatedAtAfter(any())).thenReturn(0L);
        when(notes.countByCreatedAtBetween(any(), any())).thenReturn(0L);
        when(planChanges.countDistinctUsersByTransitionSince(eq(Plan.FREE), eq(Plan.TEAM), any()))
                .thenReturn(3L);
        when(planChanges.countDistinctUsersByTransitionSince(eq(Plan.PERSONAL), eq(Plan.TEAM), any()))
                .thenReturn(0L);
        when(planChanges.countDistinctUsersByTransitionBetween(
                eq(Plan.FREE), eq(Plan.TEAM), any(), any())).thenReturn(10L);
        when(planChanges.countDistinctUsersByTransitionBetween(
                eq(Plan.PERSONAL), eq(Plan.TEAM), any(), any())).thenReturn(0L);
        when(subscriptions.countEntitledOn(any())).thenReturn(0L);

        TeamAdoptionMetrics m = service.snapshot();

        assertThat(m.freeToTeamDeltaPercent()).isEqualTo(-70);
        assertThat(m.freeToTeamDeltaLabel()).isEqualTo("▼ 70% vs. prior 30 days");
    }

    @Test
    void emptyWindowSkipsFindCreatedSinceAndReturnsZeros() {
        when(notes.countByCreatedAtAfter(any())).thenReturn(0L);
        when(planChanges.countDistinctUsersByTransitionSince(any(), any(), any())).thenReturn(0L);
        when(subscriptions.countEntitledOn(any())).thenReturn(0L);

        TeamAdoptionMetrics m = service.snapshot();

        assertThat(m.windowDays()).isEqualTo(30);
        assertThat(m.notesPosted()).isZero();
        assertThat(m.activeNoteAuthors()).isZero();
        assertThat(m.teamsWithNotes()).isZero();
        assertThat(m.mentionsWritten()).isZero();
        assertThat(m.freeToTeamConversions()).isZero();
        assertThat(m.personalToTeamConversions()).isZero();
        assertThat(m.entitledTeamSubscribers()).isZero();
        assertThat(m.totalTeamConversions()).isZero();
        assertThat(m.freeToTeamSharePercent()).isZero();
        verify(notes, never()).findCreatedSince(any());
    }

    @Test
    void distinctAuthorsAndTeamsAndMentionsAreDerivedFromWindowNotes() {
        User alice = userWithId(1L, "alice@example.com");
        User bob = userWithId(2L, "bob@example.com");
        Team teamA = teamWithId(100L);
        Team teamB = teamWithId(200L);
        EmailThread thread = new EmailThread(alice, "Subj", "<m@x>");

        when(notes.countByCreatedAtAfter(any())).thenReturn(4L);
        when(notes.findCreatedSince(any())).thenReturn(List.of(
                noteOf(thread, teamA, alice, "Quick check @bob — can you take this?"),
                noteOf(thread, teamA, alice, "Followup, also @bob.smith on this"),
                noteOf(thread, teamB, bob,   "On it, looping in @alice"),
                noteOf(thread, teamA, alice, "no mentions in this one")));
        when(planChanges.countDistinctUsersByTransitionSince(any(), any(), any())).thenReturn(0L);
        when(subscriptions.countEntitledOn(any())).thenReturn(0L);

        TeamAdoptionMetrics m = service.snapshot();

        assertThat(m.notesPosted()).isEqualTo(4);
        assertThat(m.activeNoteAuthors()).isEqualTo(2);
        assertThat(m.teamsWithNotes()).isEqualTo(2);
        assertThat(m.mentionsWritten()).isEqualTo(3);
    }

    @Test
    void freeToTeamAndPersonalToTeamAreBucketedSeparately() {
        when(notes.countByCreatedAtAfter(any())).thenReturn(0L);
        when(planChanges.countDistinctUsersByTransitionSince(eq(Plan.FREE), eq(Plan.TEAM), any()))
                .thenReturn(7L);
        when(planChanges.countDistinctUsersByTransitionSince(eq(Plan.PERSONAL), eq(Plan.TEAM), any()))
                .thenReturn(3L);
        when(subscriptions.countEntitledOn(any())).thenReturn(0L);

        TeamAdoptionMetrics m = service.snapshot();

        assertThat(m.freeToTeamConversions()).isEqualTo(7);
        assertThat(m.personalToTeamConversions()).isEqualTo(3);
        assertThat(m.totalTeamConversions()).isEqualTo(10);
        assertThat(m.freeToTeamSharePercent()).isEqualTo(70);
        assertThat(m.personalToTeamSharePercent()).isEqualTo(30);
    }

    @Test
    void zeroTeamConversionsRendersZeroShareNotDivideByZero() {
        when(notes.countByCreatedAtAfter(any())).thenReturn(0L);
        when(planChanges.countDistinctUsersByTransitionSince(any(), any(), any())).thenReturn(0L);
        when(subscriptions.countEntitledOn(any())).thenReturn(0L);

        TeamAdoptionMetrics m = service.snapshot();

        assertThat(m.totalTeamConversions()).isZero();
        assertThat(m.freeToTeamSharePercent()).isZero();
        assertThat(m.personalToTeamSharePercent()).isZero();
    }

    @Test
    void entitledTeamAndEnterpriseSubscriberCountsAreLookedUpPerPlan() {
        when(notes.countByCreatedAtAfter(any())).thenReturn(0L);
        when(planChanges.countDistinctUsersByTransitionSince(any(), any(), any())).thenReturn(0L);
        when(subscriptions.countEntitledOn(Plan.TEAM)).thenReturn(12L);
        when(subscriptions.countEntitledOn(Plan.ENTERPRISE)).thenReturn(2L);

        TeamAdoptionMetrics m = service.snapshot();

        assertThat(m.entitledTeamSubscribers()).isEqualTo(12);
        assertThat(m.entitledEnterpriseSubscribers()).isEqualTo(2);
    }

    @Test
    void mentionRegexIgnoresAtSignInsideEmailAddresses() {
        long n = TeamAdoptionMetricsService.countMentionTokens(
                "ping jane@example.com but also @jane please");
        // Only "@jane" — the at-sign in the email is preceded by a handle
        // character so the negative lookbehind drops it.
        assertThat(n).isEqualTo(1);
    }

    private ThreadNote noteOf(EmailThread thread, Team team, User author, String body) {
        return new ThreadNote(thread, team, author, body);
    }

    private static User userWithId(Long id, String email) {
        User u = new User(email, "hash", null);
        setField(u, "id", id);
        return u;
    }

    private static Team teamWithId(Long id) {
        User owner = userWithId(9_000L + id, "owner" + id + "@example.com");
        Team team = new Team("team-" + id, owner);
        setField(team, "id", id);
        return team;
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Class<?> c = target.getClass();
            while (c != null) {
                try {
                    Field f = c.getDeclaredField(name);
                    f.setAccessible(true);
                    f.set(target, value);
                    return;
                } catch (NoSuchFieldException e) {
                    c = c.getSuperclass();
                }
            }
            throw new NoSuchFieldException(name);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
