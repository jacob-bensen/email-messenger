package com.emailmessenger.admin;

import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.ThreadNote;
import com.emailmessenger.repository.PlanChangeEventRepository;
import com.emailmessenger.repository.SubscriptionRepository;
import com.emailmessenger.repository.ThreadNoteRepository;
import com.emailmessenger.team.NoteMentionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * EPIC-16 M4 "Team-plan adoption — last 30 days" card.
 *
 * <p>Joins three signals into one snapshot: (a) raw engagement on the
 * notes surface ({@link ThreadNoteRepository} window scan), (b) parsed
 * @-mention count via the same regex {@link NoteMentionService} uses to
 * fan out emails — so the operator number matches what teammates actually
 * see in their inbox, (c) plan-transition conversions logged by
 * {@link com.emailmessenger.billing.BillingService} into
 * {@code plan_change_events}, bucketed by from-plan.
 */
@Service
public class TeamAdoptionMetricsService {

    static final int WINDOW_DAYS = 30;

    private final ThreadNoteRepository notes;
    private final PlanChangeEventRepository planChanges;
    private final SubscriptionRepository subscriptions;
    private final Clock clock;

    TeamAdoptionMetricsService(ThreadNoteRepository notes,
                               PlanChangeEventRepository planChanges,
                               SubscriptionRepository subscriptions,
                               Clock clock) {
        this.notes = notes;
        this.planChanges = planChanges;
        this.subscriptions = subscriptions;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public TeamAdoptionMetrics snapshot() {
        LocalDateTime cutoff = LocalDateTime.now(clock).minusDays(WINDOW_DAYS);

        long notesPosted = notes.countByCreatedAtAfter(cutoff);
        List<ThreadNote> windowNotes = notesPosted == 0
                ? List.of()
                : notes.findCreatedSince(cutoff);

        Set<Long> distinctAuthors = new HashSet<>();
        Set<Long> distinctTeams = new HashSet<>();
        long mentionsWritten = 0;
        for (ThreadNote n : windowNotes) {
            if (n.getAuthorUser() != null && n.getAuthorUser().getId() != null) {
                distinctAuthors.add(n.getAuthorUser().getId());
            }
            if (n.getTeam() != null && n.getTeam().getId() != null) {
                distinctTeams.add(n.getTeam().getId());
            }
            mentionsWritten += countMentionTokens(n.getBody());
        }

        long freeToTeam = planChanges.countDistinctUsersByTransitionSince(
                Plan.FREE, Plan.TEAM, cutoff);
        long personalToTeam = planChanges.countDistinctUsersByTransitionSince(
                Plan.PERSONAL, Plan.TEAM, cutoff);
        long entitledTeam = subscriptions.countEntitledOn(Plan.TEAM);
        long entitledEnterprise = subscriptions.countEntitledOn(Plan.ENTERPRISE);

        return new TeamAdoptionMetrics(
                WINDOW_DAYS,
                notesPosted,
                distinctAuthors.size(),
                distinctTeams.size(),
                mentionsWritten,
                freeToTeam,
                personalToTeam,
                entitledTeam,
                entitledEnterprise);
    }

    static long countMentionTokens(String body) {
        if (body == null || body.isEmpty()) {
            return 0;
        }
        Matcher m = NoteMentionService.MENTION_TOKEN.matcher(body);
        long n = 0;
        while (m.find()) {
            n++;
        }
        return n;
    }
}
