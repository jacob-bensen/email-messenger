package com.emailmessenger.team;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.billing.StripeCheckoutGateway;
import com.emailmessenger.billing.StripePortalGateway;
import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.Team;
import com.emailmessenger.domain.TeamMember;
import com.emailmessenger.domain.TeamMemberRole;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.repository.TeamMemberRepository;
import com.emailmessenger.repository.TeamRepository;
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
class ThreadAccessServiceTest {

    @Autowired ThreadAccessService threadAccess;
    @Autowired UserService userService;
    @Autowired EmailThreadRepository threads;
    @Autowired TeamRepository teamRepository;
    @Autowired TeamMemberRepository teamMembers;

    @MockBean JavaMailSender mailSender;
    @MockBean StripeCheckoutGateway stripeCheckout;
    @MockBean StripePortalGateway stripePortal;
    @MockBean ReplyService replyService;

    private User newUser(String email) {
        userService.register(email, "secret-password", email);
        return userService.findByEmail(email).orElseThrow();
    }

    private EmailThread newThreadOwnedBy(User owner) {
        return threads.save(new EmailThread(owner, "Subject", "<root-" + owner.getId() + "@test>"));
    }

    private Team joinTeam(User owner, User teammate, TeamMemberRole role) {
        Team team = teamRepository.findByOwnerUser(owner).orElseGet(() -> {
            Team created = teamRepository.save(new Team(owner.getEmail() + "'s team", owner));
            teamMembers.save(new TeamMember(created, owner, TeamMemberRole.OWNER));
            return created;
        });
        if (teammate != null) {
            teamMembers.save(new TeamMember(team, teammate, role));
        }
        return team;
    }

    @Test
    void ownerCanAlwaysAccessOwnThread() {
        User owner = newUser("owner-access@example.com");
        EmailThread thread = newThreadOwnedBy(owner);

        assertThat(threadAccess.isAccessibleTo(thread, owner)).isTrue();
        assertThat(threadAccess.isOwner(thread, owner)).isTrue();
        assertThat(threadAccess.findAccessibleThread(thread.getId(), owner)).isPresent();
    }

    @Test
    void strangerWithNoTeamMembershipCannotAccess() {
        User owner = newUser("owner-stranger@example.com");
        User stranger = newUser("stranger-access@example.com");
        EmailThread thread = newThreadOwnedBy(owner);

        assertThat(threadAccess.isAccessibleTo(thread, stranger)).isFalse();
        assertThat(threadAccess.findAccessibleThread(thread.getId(), stranger)).isEmpty();
    }

    @Test
    void teammateCanAccessOwnerThreadAndIsNotMarkedOwner() {
        User owner = newUser("owner-share@example.com");
        User teammate = newUser("teammate-share@example.com");
        EmailThread thread = newThreadOwnedBy(owner);
        joinTeam(owner, teammate, TeamMemberRole.MEMBER);

        assertThat(threadAccess.isAccessibleTo(thread, teammate)).isTrue();
        assertThat(threadAccess.isOwner(thread, teammate)).isFalse();
        assertThat(threadAccess.findAccessibleThread(thread.getId(), teammate)).isPresent();
    }

    @Test
    void teammateOfDifferentTeamCannotAccess() {
        User aOwner = newUser("ateam-owner@example.com");
        User bOwner = newUser("bteam-owner@example.com");
        User bMember = newUser("bteam-member@example.com");
        EmailThread aThread = newThreadOwnedBy(aOwner);
        joinTeam(aOwner, null, TeamMemberRole.OWNER);
        joinTeam(bOwner, bMember, TeamMemberRole.MEMBER);

        assertThat(threadAccess.isAccessibleTo(aThread, bMember)).isFalse();
    }

    @Test
    void unknownThreadIdReturnsEmpty() {
        User viewer = newUser("anyone-access@example.com");

        assertThat(threadAccess.findAccessibleThread(9_999_999L, viewer)).isEmpty();
    }
}
