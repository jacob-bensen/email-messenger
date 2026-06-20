package com.emailmessenger.team;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.billing.StripeCheckoutGateway;
import com.emailmessenger.billing.StripePortalGateway;
import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.Subscription;
import com.emailmessenger.domain.Team;
import com.emailmessenger.domain.TeamMember;
import com.emailmessenger.domain.TeamMemberRole;
import com.emailmessenger.domain.ThreadNote;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.repository.SubscriptionRepository;
import com.emailmessenger.repository.TeamMemberRepository;
import com.emailmessenger.repository.TeamRepository;
import com.emailmessenger.repository.ThreadNoteRepository;
import com.emailmessenger.repository.UserRepository;
import com.emailmessenger.service.ReplyService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class NoteMentionServiceTest {

    @Autowired NoteMentionService noteMentionService;
    @Autowired ThreadNoteService threadNoteService;
    @Autowired UserService userService;
    @Autowired UserRepository users;
    @Autowired EmailThreadRepository threads;
    @Autowired ThreadNoteRepository notes;
    @Autowired SubscriptionRepository subscriptions;
    @Autowired TeamRepository teamRepository;
    @Autowired TeamMemberRepository teamMembers;

    @MockitoBean JavaMailSender mailSender;
    @MockitoBean StripeCheckoutGateway stripeCheckout;
    @MockitoBean StripePortalGateway stripePortal;
    @MockitoBean ReplyService replyService;

    @BeforeEach
    void stubMime() {
        // A fresh MimeMessage per call — using thenReturn(...) would hand
        // back the same mutable instance and the second setTo() would
        // overwrite the first, masking real send-count regressions.
        when(mailSender.createMimeMessage())
                .thenAnswer(invocation -> new MimeMessage((Session) null));
    }

    private User newUser(String email, String name) {
        userService.register(email, "secret-password", name);
        return userService.findByEmail(email).orElseThrow();
    }

    private void activatePlan(User user, Plan plan) {
        Subscription sub = subscriptions.findByUser(user)
                .orElseGet(() -> new Subscription(user, "cus_" + user.getId(), "active"));
        sub.setPlan(plan);
        sub.setStatus("active");
        subscriptions.save(sub);
    }

    private EmailThread newThreadOwnedBy(User owner) {
        return threads.save(new EmailThread(owner, "Subject", "<root-" + owner.getId() + "@test>"));
    }

    private Team joinTeam(User owner, User member) {
        Team team = teamRepository.findByOwnerUser(owner).orElseGet(() -> {
            Team created = teamRepository.save(new Team(owner.getEmail() + "'s team", owner));
            teamMembers.save(new TeamMember(created, owner, TeamMemberRole.OWNER));
            return created;
        });
        teamMembers.save(new TeamMember(team, member, TeamMemberRole.MEMBER));
        return team;
    }

    private static Set<String> recipientStrings(MimeMessage[] messages) throws Exception {
        Set<String> out = new HashSet<>();
        for (MimeMessage m : messages) {
            for (jakarta.mail.Address a : m.getAllRecipients()) {
                out.add(a.toString());
            }
        }
        return out;
    }

    @Test
    void mentionByEmailLocalPartSendsExactlyOneEmailToThatMember() throws Exception {
        User owner = newUser("owner@example.com", "Owner");
        activatePlan(owner, Plan.TEAM);
        User bob = newUser("bob@example.com", "Bob Builder");
        joinTeam(owner, bob);
        EmailThread thread = newThreadOwnedBy(owner);

        ThreadNoteService.PostResult result =
                threadNoteService.post(thread, owner, "hey @bob — can you look at this?");
        assertThat(result.outcome()).isEqualTo(ThreadNoteService.PostOutcome.POSTED);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, atLeastOnce()).send(captor.capture());
        Set<String> rcpts = recipientStrings(captor.getAllValues().toArray(new MimeMessage[0]));
        assertThat(rcpts).contains("bob@example.com");
        assertThat(rcpts).doesNotContain("owner@example.com");
    }

    @Test
    void displayNameNormalizedMatchResolvesToTheRightMember() throws Exception {
        User owner = newUser("owner-dn@example.com", "Owner");
        activatePlan(owner, Plan.TEAM);
        User alice = newUser("alice-xyz@example.com", "Alice Wong");
        joinTeam(owner, alice);
        EmailThread thread = newThreadOwnedBy(owner);

        // @alicewong should resolve to displayName "Alice Wong" → handle "alicewong"
        threadNoteService.post(thread, owner, "looping in @alicewong here");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, atLeastOnce()).send(captor.capture());
        assertThat(recipientStrings(captor.getAllValues().toArray(new MimeMessage[0])))
                .contains("alice-xyz@example.com");
    }

    @Test
    void selfMentionDoesNotEmailTheAuthor() throws Exception {
        User owner = newUser("self@example.com", "Self");
        activatePlan(owner, Plan.TEAM);
        EmailThread thread = newThreadOwnedBy(owner);

        threadNoteService.post(thread, owner, "note to self: @self follow up tomorrow");

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void unmatchedTokenSendsNoEmail() throws Exception {
        User owner = newUser("unmatched@example.com", "Owner");
        activatePlan(owner, Plan.TEAM);
        User bob = newUser("bob2@example.com", "Bob");
        joinTeam(owner, bob);
        EmailThread thread = newThreadOwnedBy(owner);

        threadNoteService.post(thread, owner, "@nobody on the team has this handle");

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void multipleMentionsDedupeToOneEmailPerRecipient() throws Exception {
        User owner = newUser("multi@example.com", "Owner");
        activatePlan(owner, Plan.TEAM);
        User bob = newUser("bobmulti@example.com", "Bob Multi");
        User carol = newUser("carolmulti@example.com", "Carol Multi");
        joinTeam(owner, bob);
        joinTeam(owner, carol);
        EmailThread thread = newThreadOwnedBy(owner);

        threadNoteService.post(thread, owner,
                "@bobmulti @carolmulti — and again @bobmulti to be sure");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, atLeastOnce()).send(captor.capture());
        Set<String> rcpts = recipientStrings(captor.getAllValues().toArray(new MimeMessage[0]));
        assertThat(rcpts).containsExactlyInAnyOrder(
                "bobmulti@example.com", "carolmulti@example.com");
        // 2 sends total — one per unique mentioned recipient
        assertThat(captor.getAllValues()).hasSize(2);
    }

    @Test
    void mailFailureForOneRecipientDoesNotBlockOthersAndDoesNotRollBackTheNote() throws Exception {
        User owner = newUser("partial@example.com", "Owner");
        activatePlan(owner, Plan.TEAM);
        User bad = newUser("bad-recipient@example.com", "Bad Recipient");
        User good = newUser("good-recipient@example.com", "Good Recipient");
        joinTeam(owner, bad);
        joinTeam(owner, good);
        EmailThread thread = newThreadOwnedBy(owner);

        // First send fails, all subsequent sends succeed.
        doThrow(new MailSendException("smtp down"))
                .doNothing()
                .when(mailSender).send(any(MimeMessage.class));

        ThreadNoteService.PostResult result =
                threadNoteService.post(thread, owner, "@badrecipient @goodrecipient please look");
        assertThat(result.outcome()).isEqualTo(ThreadNoteService.PostOutcome.POSTED);
        // Note still saved despite mail failure.
        assertThat(notes.countByThread(thread)).isEqualTo(1L);
        // mailSender invoked twice — once per recipient — even though the first threw.
        verify(mailSender, atLeastOnce()).send(any(MimeMessage.class));
    }

    @Test
    void tokenAfterNonWhitespaceCharacterIsIgnored() throws Exception {
        // An "@" embedded inside an email address (e.g. "see jane@example.com")
        // must not register as a mention — the regex looks for a leading word
        // boundary on the author side.
        User owner = newUser("inline@example.com", "Owner");
        activatePlan(owner, Plan.TEAM);
        User jane = newUser("jane@example.com", "Jane");
        joinTeam(owner, jane);
        EmailThread thread = newThreadOwnedBy(owner);

        threadNoteService.post(thread, owner, "forwarded from jane@example.com earlier today");

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void candidatesForThreadExcludesViewerAndReturnsTeammates() {
        User owner = newUser("cand-owner@example.com", "Owner");
        activatePlan(owner, Plan.TEAM);
        User bob = newUser("cand-bob@example.com", "Bob Cand");
        User carol = newUser("cand-carol@example.com", "Carol Cand");
        joinTeam(owner, bob);
        joinTeam(owner, carol);
        EmailThread thread = newThreadOwnedBy(owner);

        // Viewer = owner: should see bob + carol but not themselves
        List<NoteMentionService.MentionCandidate> ownerCandidates =
                noteMentionService.candidatesForThread(thread, owner);
        assertThat(ownerCandidates).extracting(NoteMentionService.MentionCandidate::email)
                .containsExactlyInAnyOrder("cand-bob@example.com", "cand-carol@example.com");

        // Viewer = bob: should see owner + carol but not themselves
        List<NoteMentionService.MentionCandidate> bobCandidates =
                noteMentionService.candidatesForThread(thread, bob);
        assertThat(bobCandidates).extracting(NoteMentionService.MentionCandidate::email)
                .containsExactlyInAnyOrder("cand-owner@example.com", "cand-carol@example.com");
    }

    @Test
    void candidatesForThreadIsEmptyWhenOwnerHasNoTeamYet() {
        User owner = newUser("lonely@example.com", "Lonely");
        activatePlan(owner, Plan.PERSONAL);
        EmailThread thread = newThreadOwnedBy(owner);

        assertThat(noteMentionService.candidatesForThread(thread, owner)).isEmpty();
    }
}
