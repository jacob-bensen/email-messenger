package com.emailmessenger.team;

import com.emailmessenger.billing.PlanLimitService;
import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.Team;
import com.emailmessenger.domain.ThreadNote;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.ThreadNoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Internal team notes on a thread.
 *
 * <p>EPIC-16 M2: notes are visible to (and writable by) any member of the
 * thread-owner's team — not just the owner. Plan gating is anchored on the
 * owner (they pay for the seat), so an invited teammate on a personal Free
 * plan can still drop notes inside a Team-plan owner's thread.
 */
@Service
public class ThreadNoteService {

    public static final int MAX_BODY_LENGTH = 4_000;

    private static final Set<Plan> TEAM_PLANS = Set.of(Plan.TEAM, Plan.ENTERPRISE);

    private final ThreadNoteRepository notes;
    private final TeamService teamService;
    private final ThreadAccessService threadAccess;
    private final PlanLimitService planLimits;

    ThreadNoteService(ThreadNoteRepository notes,
                      TeamService teamService,
                      ThreadAccessService threadAccess,
                      PlanLimitService planLimits) {
        this.notes = notes;
        this.teamService = teamService;
        this.threadAccess = threadAccess;
        this.planLimits = planLimits;
    }

    public enum PostOutcome { POSTED, GATED, BLANK, TOO_LONG }

    public record PostResult(PostOutcome outcome, ThreadNote note) {
        public static PostResult of(PostOutcome outcome) { return new PostResult(outcome, null); }
        public static PostResult posted(ThreadNote note) { return new PostResult(PostOutcome.POSTED, note); }
    }

    /**
     * Owner-side gate: does {@code viewer}'s own plan include notes? Used to
     * decide whether to show the "Upgrade to Team" CTA on the viewer's own
     * threads. Teammate-side access goes through {@link #canAccessNotesOn}.
     */
    @Transactional(readOnly = true)
    public boolean canAccessNotes(User viewer) {
        return TEAM_PLANS.contains(planLimits.currentPlan(viewer));
    }

    /**
     * Can {@code viewer} see/post notes on this thread? True when the
     * thread's owner is on a Team/Enterprise plan and the viewer is the
     * owner or in the owner's team.
     */
    @Transactional(readOnly = true)
    public boolean canAccessNotesOn(EmailThread thread, User viewer) {
        if (!TEAM_PLANS.contains(planLimits.currentPlan(thread.getOwner()))) {
            return false;
        }
        return threadAccess.isAccessibleTo(thread, viewer);
    }

    /**
     * Notes visible to {@code viewer} on this thread.
     */
    @Transactional(readOnly = true)
    public List<ThreadNote> notesFor(EmailThread thread, User viewer) {
        if (!canAccessNotesOn(thread, viewer)) {
            return List.of();
        }
        return notes.findByThreadOrderByCreatedAtAsc(thread);
    }

    /**
     * Add a note to {@code thread} authored by {@code author}. Returns
     * {@link PostOutcome#GATED} when the author can't access notes on this
     * thread (owner not on a Team plan, or author is neither owner nor a
     * member of the owner's team).
     */
    @Transactional
    public PostResult post(EmailThread thread, User author, String body) {
        if (!canAccessNotesOn(thread, author)) {
            return PostResult.of(PostOutcome.GATED);
        }
        if (body == null) {
            return PostResult.of(PostOutcome.BLANK);
        }
        String trimmed = body.trim();
        if (trimmed.isEmpty()) {
            return PostResult.of(PostOutcome.BLANK);
        }
        if (trimmed.length() > MAX_BODY_LENGTH) {
            return PostResult.of(PostOutcome.TOO_LONG);
        }
        Team team = teamService.findOrCreateOwnedTeam(thread.getOwner());
        ThreadNote saved = notes.save(new ThreadNote(thread, team, author, trimmed));
        return PostResult.posted(saved);
    }
}
