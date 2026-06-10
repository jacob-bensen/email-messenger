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
 * <p>Notes are the first concrete shared-inbox feature that justifies
 * the Team plan: an author drops a private comment on a thread; other
 * team members will see it (M2 extends the view to invitees). M1 ships
 * the owner-side surface: the thread owner sees their own notes, and
 * the feature is gated to TEAM / ENTERPRISE plans so Free / Personal
 * users hit an upgrade CTA in the same spot.
 */
@Service
public class ThreadNoteService {

    public static final int MAX_BODY_LENGTH = 4_000;

    private static final Set<Plan> TEAM_PLANS = Set.of(Plan.TEAM, Plan.ENTERPRISE);

    private final ThreadNoteRepository notes;
    private final TeamService teamService;
    private final PlanLimitService planLimits;

    ThreadNoteService(ThreadNoteRepository notes,
                      TeamService teamService,
                      PlanLimitService planLimits) {
        this.notes = notes;
        this.teamService = teamService;
        this.planLimits = planLimits;
    }

    public enum PostOutcome { POSTED, GATED, BLANK, TOO_LONG }

    public record PostResult(PostOutcome outcome, ThreadNote note) {
        public static PostResult of(PostOutcome outcome) { return new PostResult(outcome, null); }
        public static PostResult posted(ThreadNote note) { return new PostResult(PostOutcome.POSTED, note); }
    }

    /**
     * Is the viewer on a plan that includes internal team notes? Drives
     * both the panel render (Team-only textarea vs upgrade CTA) and the
     * server-side post gate so a tampered form can't bypass the modal.
     */
    @Transactional(readOnly = true)
    public boolean canAccessNotes(User viewer) {
        return TEAM_PLANS.contains(planLimits.currentPlan(viewer));
    }

    /**
     * Notes visible to {@code viewer} on this thread. M1 only returns
     * rows when the viewer owns the thread and is on a Team plan; M2
     * will extend this to fellow team members following a shared link.
     */
    @Transactional(readOnly = true)
    public List<ThreadNote> notesFor(EmailThread thread, User viewer) {
        if (!canAccessNotes(viewer)) {
            return List.of();
        }
        if (!ownsThread(viewer, thread)) {
            return List.of();
        }
        return notes.findByThreadOrderByCreatedAtAsc(thread);
    }

    /**
     * Add a note to {@code thread} authored by {@code author}. Returns
     * {@link PostOutcome#GATED} when the author isn't on a Team plan or
     * doesn't own the thread (M1 only — M2 widens to teammates), so the
     * controller can surface the same upgrade CTA the panel already shows.
     */
    @Transactional
    public PostResult post(EmailThread thread, User author, String body) {
        if (!canAccessNotes(author) || !ownsThread(author, thread)) {
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

    private static boolean ownsThread(User user, EmailThread thread) {
        Long ownerId = thread.getOwner().getId();
        return ownerId != null && ownerId.equals(user.getId());
    }
}
