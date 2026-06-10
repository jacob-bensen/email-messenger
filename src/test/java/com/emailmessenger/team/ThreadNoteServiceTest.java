package com.emailmessenger.team;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.billing.StripeCheckoutGateway;
import com.emailmessenger.billing.StripePortalGateway;
import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.Subscription;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.repository.SubscriptionRepository;
import com.emailmessenger.repository.ThreadNoteRepository;
import com.emailmessenger.repository.UserRepository;
import com.emailmessenger.service.ReplyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class ThreadNoteServiceTest {

    @Autowired ThreadNoteService threadNoteService;
    @Autowired UserService userService;
    @Autowired UserRepository users;
    @Autowired EmailThreadRepository threads;
    @Autowired ThreadNoteRepository notes;
    @Autowired SubscriptionRepository subscriptions;

    @MockBean JavaMailSender mailSender;
    @MockBean StripeCheckoutGateway stripeCheckout;
    @MockBean StripePortalGateway stripePortal;
    @MockBean ReplyService replyService;

    private User newUser(String email, String name) {
        userService.register(email, "secret-password", name);
        return userService.findByEmail(email).orElseThrow();
    }

    private void activatePlan(User user, Plan plan) {
        Subscription sub = subscriptions.findByUser(user)
                .orElseGet(() -> new Subscription(user, "cus_test_" + user.getId(), "active"));
        sub.setPlan(plan);
        sub.setStatus("active");
        subscriptions.save(sub);
    }

    private EmailThread newThreadOwnedBy(User owner) {
        EmailThread thread = new EmailThread(owner, "Subject", "<root-" + owner.getId() + "@test>");
        return threads.save(thread);
    }

    @Test
    void freeUserCannotAccessNotes() {
        User free = newUser("free@example.com", "Free");
        assertThat(threadNoteService.canAccessNotes(free)).isFalse();
    }

    @Test
    void personalUserCannotAccessNotes() {
        User personal = newUser("personal@example.com", "Personal");
        activatePlan(personal, Plan.PERSONAL);
        assertThat(threadNoteService.canAccessNotes(personal)).isFalse();
    }

    @Test
    void teamUserCanAccessNotes() {
        User team = newUser("team@example.com", "Team");
        activatePlan(team, Plan.TEAM);
        assertThat(threadNoteService.canAccessNotes(team)).isTrue();
    }

    @Test
    void postOnFreePlanReturnsGatedAndPersistsNothing() {
        User free = newUser("freepost@example.com", "Free");
        EmailThread thread = newThreadOwnedBy(free);

        ThreadNoteService.PostResult result =
                threadNoteService.post(thread, free, "Hello team");

        assertThat(result.outcome()).isEqualTo(ThreadNoteService.PostOutcome.GATED);
        assertThat(notes.countByThread(thread)).isZero();
    }

    @Test
    void postOnTeamPlanPersistsTrimmedBodyAttributedToAuthor() {
        User team = newUser("teampost@example.com", "Team Author");
        activatePlan(team, Plan.TEAM);
        EmailThread thread = newThreadOwnedBy(team);

        ThreadNoteService.PostResult result =
                threadNoteService.post(thread, team, "   Heads-up on this thread   ");

        assertThat(result.outcome()).isEqualTo(ThreadNoteService.PostOutcome.POSTED);
        assertThat(notes.countByThread(thread)).isEqualTo(1L);
        var loaded = notes.findByThreadOrderByCreatedAtAsc(thread);
        assertThat(loaded.get(0).getBody()).isEqualTo("Heads-up on this thread");
        assertThat(loaded.get(0).getAuthorUser().getId()).isEqualTo(team.getId());
        assertThat(loaded.get(0).getCreatedAt()).isNotNull();
    }

    @Test
    void postWithBlankBodyIsRejected() {
        User team = newUser("blank@example.com", "Team");
        activatePlan(team, Plan.TEAM);
        EmailThread thread = newThreadOwnedBy(team);

        assertThat(threadNoteService.post(thread, team, "   ").outcome())
                .isEqualTo(ThreadNoteService.PostOutcome.BLANK);
        assertThat(threadNoteService.post(thread, team, null).outcome())
                .isEqualTo(ThreadNoteService.PostOutcome.BLANK);
        assertThat(notes.countByThread(thread)).isZero();
    }

    @Test
    void postOverMaxLengthIsRejected() {
        User team = newUser("long@example.com", "Team");
        activatePlan(team, Plan.TEAM);
        EmailThread thread = newThreadOwnedBy(team);
        String tooLong = "x".repeat(ThreadNoteService.MAX_BODY_LENGTH + 1);

        ThreadNoteService.PostResult result =
                threadNoteService.post(thread, team, tooLong);

        assertThat(result.outcome()).isEqualTo(ThreadNoteService.PostOutcome.TOO_LONG);
        assertThat(notes.countByThread(thread)).isZero();
    }

    @Test
    void notesForReturnsEmptyForNonOwnerEvenOnTeamPlan() {
        User owner = newUser("owner@example.com", "Owner");
        activatePlan(owner, Plan.TEAM);
        User stranger = newUser("stranger@example.com", "Stranger");
        activatePlan(stranger, Plan.TEAM);
        EmailThread thread = newThreadOwnedBy(owner);
        threadNoteService.post(thread, owner, "private note");

        assertThat(threadNoteService.notesFor(thread, stranger)).isEmpty();
        assertThat(threadNoteService.notesFor(thread, owner)).hasSize(1);
    }

    @Test
    void cancelledSubscriptionDropsBackToFreeAndLosesAccess() {
        User user = newUser("cancelled@example.com", "Cancelled");
        activatePlan(user, Plan.TEAM);
        assertThat(threadNoteService.canAccessNotes(user)).isTrue();

        Subscription sub = subscriptions.findByUser(user).orElseThrow();
        sub.setStatus("canceled");
        subscriptions.save(sub);

        assertThat(threadNoteService.canAccessNotes(user)).isFalse();
    }
}
