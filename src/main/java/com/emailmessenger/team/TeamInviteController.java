package com.emailmessenger.team;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.Team;
import com.emailmessenger.domain.TeamInvite;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.TeamInviteRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

/**
 * Endpoints for the team-invite flow.
 *
 * <p>{@code GET /team/invite} renders the invite form + outstanding
 * invite list for the signed-in user's team. {@code POST /team/invite}
 * mints a token, emails the invitee, and redirects back with a flash
 * message.
 *
 * <p>{@code GET /team/invite/accept?token=…} requires the visitor to
 * be signed in (Spring Security redirects to /login first). On success
 * it adds the user to the inviter's team and redirects to /threads
 * with a flash message.
 */
@Controller
class TeamInviteController {

    private final TeamInviteService teamInviteService;
    private final TeamService teamService;
    private final TeamInviteRepository teamInvites;
    private final UserService userService;

    TeamInviteController(TeamInviteService teamInviteService,
                         TeamService teamService,
                         TeamInviteRepository teamInvites,
                         UserService userService) {
        this.teamInviteService = teamInviteService;
        this.teamService = teamService;
        this.teamInvites = teamInvites;
        this.userService = userService;
    }

    @GetMapping("/team/invite")
    String inviteForm(Principal principal, Model model) {
        User inviter = userService.requireByEmail(principal.getName());
        Team team = teamService.findOrCreateOwnedTeam(inviter);
        List<TeamInvite> invites = teamInvites.findByTeamOrderByCreatedAtDesc(team);
        model.addAttribute("team", team);
        model.addAttribute("invites", invites);
        return "team/invite";
    }

    @PostMapping("/team/invite")
    String sendInvite(@RequestParam(value = "email", required = false) String email,
                      Principal principal,
                      RedirectAttributes redirectAttributes) {
        User inviter = userService.requireByEmail(principal.getName());
        TeamInviteService.InviteResult result = teamInviteService.invite(inviter, email);
        switch (result.outcome()) {
            case SENT -> redirectAttributes.addFlashAttribute("inviteFlash", "sent");
            case INVALID_EMAIL -> redirectAttributes.addFlashAttribute("inviteFlash", "invalidEmail");
            case SELF_INVITE -> redirectAttributes.addFlashAttribute("inviteFlash", "selfInvite");
            case ALREADY_PENDING -> redirectAttributes.addFlashAttribute("inviteFlash", "alreadyPending");
            case MAIL_FAILED -> redirectAttributes.addFlashAttribute("inviteFlash", "mailFailed");
        }
        return "redirect:/team/invite";
    }

    @GetMapping("/team/invite/accept")
    String acceptForm(@RequestParam(value = "token", required = false) String token,
                      Principal principal,
                      Model model) {
        Optional<TeamInvite> match = teamInviteService.findInviteForValidToken(token);
        if (match.isEmpty()) {
            model.addAttribute("status", "invalid");
            return "team/accept";
        }
        TeamInvite invite = match.get();
        model.addAttribute("invite", invite);
        model.addAttribute("token", token);
        if (principal != null) {
            User user = userService.findByEmail(principal.getName()).orElse(null);
            if (user != null && !invite.getInviteeEmail().equalsIgnoreCase(user.getEmail())) {
                model.addAttribute("status", "emailMismatch");
                model.addAttribute("signedInEmail", user.getEmail());
                return "team/accept";
            }
        }
        model.addAttribute("status", "ready");
        return "team/accept";
    }

    @PostMapping("/team/invite/accept")
    String acceptInvite(@RequestParam(value = "token", required = false) String token,
                        Principal principal,
                        RedirectAttributes redirectAttributes,
                        Model model) {
        User accepter = userService.requireByEmail(principal.getName());
        TeamInviteService.AcceptOutcome outcome = teamInviteService.acceptInvite(token, accepter);
        switch (outcome) {
            case ACCEPTED -> redirectAttributes.addFlashAttribute("teamFlash", "joined");
            case ALREADY_MEMBER -> redirectAttributes.addFlashAttribute("teamFlash", "alreadyMember");
            case EMAIL_MISMATCH -> {
                model.addAttribute("status", "emailMismatch");
                model.addAttribute("signedInEmail", accepter.getEmail());
                model.addAttribute("token", token);
                return "team/accept";
            }
            case INVALID -> {
                model.addAttribute("status", "invalid");
                return "team/accept";
            }
        }
        return "redirect:/threads";
    }
}
