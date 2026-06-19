package com.emailmessenger.team;

import com.emailmessenger.domain.Team;
import com.emailmessenger.domain.TeamInvite;
import com.emailmessenger.domain.TeamMember;
import com.emailmessenger.domain.TeamMemberRole;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.TeamInviteRepository;
import com.emailmessenger.repository.TeamMemberRepository;
import com.emailmessenger.web.SiteProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Team invite flow.
 *
 * <p>{@link #invite(User, String)} mints a token, persists its hash,
 * and emails the invitee an accept link. The plaintext token only
 * exists in the email body / URL so a DB dump can't be replayed.
 * Tokens are single-use and expire in 7 days — longer than password
 * reset because invite emails can sit unread for several days and the
 * accept action is non-destructive.
 *
 * <p>{@link #acceptInvite(String, User)} validates the token and adds
 * the accepting user to the inviter's team. The token is consumed
 * (accepted_at set) so the link can't be reused.
 */
@Service
public class TeamInviteService {

    private static final Logger log = LoggerFactory.getLogger(TeamInviteService.class);

    static final Duration TOKEN_TTL = Duration.ofDays(7);
    static final int TOKEN_BYTES = 32;

    private final TeamService teamService;
    private final TeamInviteRepository invites;
    private final TeamMemberRepository teamMembers;
    private final JavaMailSender mailSender;
    private final SiteProperties site;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    @Value("${spring.mail.username:noreply@conexusmail.com}")
    private String fromAddress = "noreply@conexusmail.com";

    TeamInviteService(TeamService teamService,
                      TeamInviteRepository invites,
                      TeamMemberRepository teamMembers,
                      JavaMailSender mailSender,
                      SiteProperties site,
                      Clock clock) {
        this.teamService = teamService;
        this.invites = invites;
        this.teamMembers = teamMembers;
        this.mailSender = mailSender;
        this.site = site;
        this.clock = clock;
    }

    public enum Outcome {
        SENT,
        INVALID_EMAIL,
        SELF_INVITE,
        ALREADY_PENDING,
        MAIL_FAILED
    }

    public enum AcceptOutcome {
        ACCEPTED,
        ALREADY_MEMBER,
        INVALID,
        EMAIL_MISMATCH
    }

    public record InviteResult(Outcome outcome, TeamInvite invite) {
        public static InviteResult of(Outcome outcome) { return new InviteResult(outcome, null); }
        public static InviteResult sent(TeamInvite invite) { return new InviteResult(Outcome.SENT, invite); }
    }

    /**
     * Issue an invite for the given email under the inviter's owned
     * team. Lazy-creates the team on first invite.
     */
    @Transactional
    public InviteResult invite(User inviter, String inviteeEmail) {
        String normalized = normalizeEmail(inviteeEmail);
        if (normalized == null) {
            return InviteResult.of(Outcome.INVALID_EMAIL);
        }
        if (normalized.equalsIgnoreCase(inviter.getEmail())) {
            return InviteResult.of(Outcome.SELF_INVITE);
        }
        Team team = teamService.findOrCreateOwnedTeam(inviter);
        LocalDateTime now = LocalDateTime.now(clock);
        List<TeamInvite> pending = invites.findPendingForTeamAndEmail(team, normalized, now);
        if (!pending.isEmpty()) {
            return new InviteResult(Outcome.ALREADY_PENDING, pending.get(0));
        }
        String plain = newPlainToken();
        TeamInvite invite = invites.save(new TeamInvite(
                team, inviter, normalized, sha256Hex(plain), now.plus(TOKEN_TTL)));
        try {
            mailSender.send(compose(inviter, normalized, plain));
        } catch (MailException e) {
            log.warn("Team-invite mail send failed (team={}, email={}): {}",
                    team.getId(), normalized, e.getMessage());
            return InviteResult.of(Outcome.MAIL_FAILED);
        }
        return InviteResult.sent(invite);
    }

    @Transactional(readOnly = true)
    public Optional<TeamInvite> findInviteForValidToken(String plainToken) {
        if (plainToken == null || plainToken.isBlank()) {
            return Optional.empty();
        }
        Optional<TeamInvite> match = invites.findByTokenHash(sha256Hex(plainToken));
        if (match.isEmpty()) {
            return Optional.empty();
        }
        TeamInvite invite = match.get();
        if (!invite.isPending(LocalDateTime.now(clock))) {
            return Optional.empty();
        }
        return Optional.of(invite);
    }

    /**
     * Consume the token: add the accepting user to the team and mark
     * the invite as accepted. The accepting user's email must match
     * the invitee email — otherwise a stolen link could be redeemed by
     * anyone with a ConexusMail account.
     */
    @Transactional
    public AcceptOutcome acceptInvite(String plainToken, User accepter) {
        if (plainToken == null || plainToken.isBlank() || accepter == null) {
            return AcceptOutcome.INVALID;
        }
        Optional<TeamInvite> match = invites.findByTokenHash(sha256Hex(plainToken));
        if (match.isEmpty()) {
            return AcceptOutcome.INVALID;
        }
        TeamInvite invite = match.get();
        LocalDateTime now = LocalDateTime.now(clock);
        if (!invite.isPending(now)) {
            return AcceptOutcome.INVALID;
        }
        if (!invite.getInviteeEmail().equalsIgnoreCase(accepter.getEmail())) {
            return AcceptOutcome.EMAIL_MISMATCH;
        }
        Team team = invite.getTeam();
        Optional<TeamMember> existing = teamMembers.findByTeamAndUser(team, accepter);
        if (existing.isPresent()) {
            invite.setAcceptedAt(now);
            return AcceptOutcome.ALREADY_MEMBER;
        }
        teamMembers.save(new TeamMember(team, accepter, TeamMemberRole.MEMBER));
        invite.setAcceptedAt(now);
        return AcceptOutcome.ACCEPTED;
    }

    private MimeMessage compose(User inviter, String inviteeEmail, String plainToken) {
        MimeMessage mime = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(inviteeEmail);
            helper.setSubject(renderSubject(inviter));
            helper.setText(renderBody(inviter, plainToken), false);
        } catch (MessagingException e) {
            throw new MailPreparationException("Could not compose team-invite email", e);
        }
        return mime;
    }

    private String renderSubject(User inviter) {
        return inviterLabel(inviter) + " invited you to their ConexusMail team";
    }

    private String renderBody(User inviter, String plainToken) {
        String url = site.getBaseUrl() + "/team/invite/accept?token=" + plainToken;
        return "Hi,\n\n"
                + inviterLabel(inviter) + " is using ConexusMail — an inbox that reads like a "
                + "chat — and wants you on the team.\n\n"
                + "Accept the invite within the next 7 days:\n\n"
                + url + "\n\n"
                + "If you didn't expect this, you can ignore the email — no account "
                + "will be created on your behalf.\n";
    }

    private static String inviterLabel(User inviter) {
        String name = inviter.getDisplayName();
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        return inviter.getEmail();
    }

    private String newPlainToken() {
        byte[] buf = new byte[TOKEN_BYTES];
        random.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    static String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        String trimmed = email.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty() || !trimmed.contains("@") || trimmed.contains(" ")) {
            return null;
        }
        return trimmed.length() > 254 ? trimmed.substring(0, 254) : trimmed;
    }

    static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
