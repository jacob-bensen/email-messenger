package com.emailmessenger.team;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.billing.StripeCheckoutGateway;
import com.emailmessenger.billing.StripePortalGateway;
import com.emailmessenger.domain.Team;
import com.emailmessenger.domain.TeamInvite;
import com.emailmessenger.domain.TeamMemberRole;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.TeamInviteRepository;
import com.emailmessenger.repository.TeamMemberRepository;
import com.emailmessenger.repository.TeamRepository;
import com.emailmessenger.repository.UserRepository;
import com.emailmessenger.service.ReplyService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class TeamInviteServiceTest {

    @Autowired TeamInviteService teamInviteService;
    @Autowired TeamService teamService;
    @Autowired UserService userService;
    @Autowired UserRepository users;
    @Autowired TeamRepository teams;
    @Autowired TeamInviteRepository invites;
    @Autowired TeamMemberRepository teamMembers;

    @MockBean JavaMailSender mailSender;
    @MockBean StripeCheckoutGateway stripeCheckout;
    @MockBean StripePortalGateway stripePortal;
    @MockBean ReplyService replyService;

    @BeforeEach
    void stubMimeFactory() {
        when(mailSender.createMimeMessage())
                .thenReturn(new MimeMessage((Session) null));
    }

    private User newUser(String email, String name) {
        userService.register(email, "secret-password", name);
        return userService.findByEmail(email).orElseThrow();
    }

    @Test
    void firstInviteLazilyCreatesTeamAndPersistsHashedToken() throws Exception {
        User inviter = newUser("owner@example.com", "Owner");

        TeamInviteService.InviteResult result =
                teamInviteService.invite(inviter, "Friend@Example.com");

        assertThat(result.outcome()).isEqualTo(TeamInviteService.Outcome.SENT);

        Team team = teams.findByOwnerUser(inviter).orElseThrow();
        assertThat(team.getName()).isEqualTo("Owner's team");
        assertThat(teamMembers.countByTeam(team)).isEqualTo(1L);
        assertThat(teamMembers.findByTeamAndUser(team, inviter).orElseThrow().getRole())
                .isEqualTo(TeamMemberRole.OWNER);

        List<TeamInvite> rows = invites.findAll();
        assertThat(rows).hasSize(1);
        TeamInvite invite = rows.get(0);
        assertThat(invite.getInviteeEmail()).isEqualTo("friend@example.com");
        assertThat(invite.getInvitedBy().getId()).isEqualTo(inviter.getId());

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage mime = captor.getValue();
        assertThat(mime.getAllRecipients()).hasSize(1);
        assertThat(mime.getAllRecipients()[0].toString()).isEqualTo("friend@example.com");
        String body = (String) mime.getContent();
        assertThat(body).contains("/team/invite/accept?token=");
        String plain = extractToken(body);
        assertThat(plain).isNotBlank();
        assertThat(invite.getTokenHash())
                .isEqualTo(TeamInviteService.sha256Hex(plain))
                .isNotEqualTo(plain);
    }

    @Test
    void selfInviteIsRejectedAndSendsNoEmail() {
        User inviter = newUser("self@example.com", "Self");

        TeamInviteService.InviteResult result =
                teamInviteService.invite(inviter, "Self@Example.com");

        assertThat(result.outcome()).isEqualTo(TeamInviteService.Outcome.SELF_INVITE);
        verify(mailSender, never()).send(any(MimeMessage.class));
        assertThat(invites.count()).isZero();
    }

    @Test
    void duplicatePendingInviteIsRejectedWithoutMintingNewToken() {
        User inviter = newUser("dup@example.com", "Dup");
        TeamInviteService.InviteResult first =
                teamInviteService.invite(inviter, "target@example.com");
        assertThat(first.outcome()).isEqualTo(TeamInviteService.Outcome.SENT);
        org.mockito.Mockito.reset(mailSender);
        when(mailSender.createMimeMessage())
                .thenReturn(new MimeMessage((Session) null));

        TeamInviteService.InviteResult second =
                teamInviteService.invite(inviter, "TARGET@example.com");

        assertThat(second.outcome()).isEqualTo(TeamInviteService.Outcome.ALREADY_PENDING);
        verify(mailSender, never()).send(any(MimeMessage.class));
        assertThat(invites.count()).isOne();
    }

    @Test
    void invalidEmailIsRejectedAndSendsNoEmail() {
        User inviter = newUser("owner2@example.com", "Owner");

        TeamInviteService.InviteResult result =
                teamInviteService.invite(inviter, "not-an-email");

        assertThat(result.outcome()).isEqualTo(TeamInviteService.Outcome.INVALID_EMAIL);
        verify(mailSender, never()).send(any(MimeMessage.class));
        assertThat(invites.count()).isZero();
    }

    @Test
    void countNonRevokedByInviterIsZeroForRevokedOnly() {
        User inviter = newUser("rev@example.com", "Rev");
        teamInviteService.invite(inviter, "x@example.com");
        TeamInvite invite = invites.findAll().get(0);
        invite.setRevokedAt(java.time.LocalDateTime.now());
        invites.save(invite);

        assertThat(invites.countNonRevokedByInviter(inviter)).isZero();
    }

    @Test
    void acceptInviteAddsAccepterToTeamAndMarksTokenUsed() throws Exception {
        User inviter = newUser("ownerA@example.com", "Owner");
        User accepter = newUser("invitee@example.com", "Invitee");
        teamInviteService.invite(inviter, "invitee@example.com");
        String plain = extractTokenFromCapturedEmail();

        TeamInviteService.AcceptOutcome outcome =
                teamInviteService.acceptInvite(plain, accepter);

        assertThat(outcome).isEqualTo(TeamInviteService.AcceptOutcome.ACCEPTED);
        Team team = teams.findByOwnerUser(inviter).orElseThrow();
        assertThat(teamMembers.countByTeam(team)).isEqualTo(2L);
        assertThat(teamMembers.findByTeamAndUser(team, accepter).orElseThrow().getRole())
                .isEqualTo(TeamMemberRole.MEMBER);
        TeamInvite row = invites.findAll().get(0);
        assertThat(row.getAcceptedAt()).isNotNull();
    }

    @Test
    void acceptRejectsTokenWhenSignedInEmailDoesNotMatchInvitee() throws Exception {
        User inviter = newUser("ownerB@example.com", "Owner");
        User wrongUser = newUser("stranger@example.com", "Stranger");
        teamInviteService.invite(inviter, "invited@example.com");
        String plain = extractTokenFromCapturedEmail();

        TeamInviteService.AcceptOutcome outcome =
                teamInviteService.acceptInvite(plain, wrongUser);

        assertThat(outcome).isEqualTo(TeamInviteService.AcceptOutcome.EMAIL_MISMATCH);
        // Invite is NOT marked accepted — the rightful invitee can still use it.
        TeamInvite row = invites.findAll().get(0);
        assertThat(row.getAcceptedAt()).isNull();
        Team team = teams.findByOwnerUser(inviter).orElseThrow();
        assertThat(teamMembers.countByTeam(team)).isEqualTo(1L);
    }

    @Test
    void acceptingTokenTwiceIsRejected() throws Exception {
        User inviter = newUser("ownerC@example.com", "Owner");
        User accepter = newUser("once@example.com", "Once");
        teamInviteService.invite(inviter, "once@example.com");
        String plain = extractTokenFromCapturedEmail();

        assertThat(teamInviteService.acceptInvite(plain, accepter))
                .isEqualTo(TeamInviteService.AcceptOutcome.ACCEPTED);
        assertThat(teamInviteService.acceptInvite(plain, accepter))
                .isEqualTo(TeamInviteService.AcceptOutcome.INVALID);
    }

    @Test
    void expiredTokenIsRejected() {
        User inviter = newUser("ownerD@example.com", "Owner");
        User accepter = newUser("late@example.com", "Late");
        Team team = teamService.findOrCreateOwnedTeam(inviter);
        String plain = "expired-token-for-test";
        invites.save(new TeamInvite(team, inviter, "late@example.com",
                TeamInviteService.sha256Hex(plain),
                java.time.LocalDateTime.now().minusDays(1)));

        assertThat(teamInviteService.findInviteForValidToken(plain)).isEmpty();
        assertThat(teamInviteService.acceptInvite(plain, accepter))
                .isEqualTo(TeamInviteService.AcceptOutcome.INVALID);
    }

    private String extractTokenFromCapturedEmail() throws Exception {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        String body = (String) captor.getValue().getContent();
        return extractToken(body);
    }

    private static String extractToken(String body) {
        int idx = body.indexOf("/team/invite/accept?token=");
        if (idx < 0) return null;
        int start = idx + "/team/invite/accept?token=".length();
        int end = start;
        while (end < body.length()) {
            char c = body.charAt(end);
            if (Character.isLetterOrDigit(c) || c == '-' || c == '_') {
                end++;
            } else {
                break;
            }
        }
        return body.substring(start, end);
    }
}
