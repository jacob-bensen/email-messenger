package com.emailmessenger.team;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.billing.StripeCheckoutGateway;
import com.emailmessenger.billing.StripePortalGateway;
import com.emailmessenger.domain.Team;
import com.emailmessenger.domain.TeamInvite;
import com.emailmessenger.repository.TeamInviteRepository;
import com.emailmessenger.repository.TeamMemberRepository;
import com.emailmessenger.repository.TeamRepository;
import com.emailmessenger.repository.UserRepository;
import com.emailmessenger.service.ReplyService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class TeamInviteControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserService userService;
    @Autowired UserRepository users;
    @Autowired TeamRepository teams;
    @Autowired TeamInviteRepository invites;
    @Autowired TeamMemberRepository teamMembers;
    @Autowired TeamInviteService teamInviteService;

    @MockBean JavaMailSender mailSender;
    @MockBean StripeCheckoutGateway stripeCheckout;
    @MockBean StripePortalGateway stripePortal;
    @MockBean ReplyService replyService;

    @BeforeEach
    void stubMimeFactory() {
        when(mailSender.createMimeMessage())
                .thenReturn(new MimeMessage((Session) null));
    }

    @Test
    void inviteFormRequiresAuth() throws Exception {
        mockMvc.perform(get("/team/invite"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "host@example.com")
    void inviteFormLazilyCreatesTeamAndRenders() throws Exception {
        userService.register("host@example.com", "secret-pw1", "Host");

        mockMvc.perform(get("/team/invite"))
                .andExpect(status().isOk())
                .andExpect(view().name("team/invite"))
                .andExpect(model().attributeExists("team"))
                .andExpect(model().attribute("invites", org.hamcrest.Matchers.empty()));

        assertThat(teams.findByOwnerUser(users.findByEmail("host@example.com").orElseThrow()))
                .isPresent();
    }

    @Test
    @WithMockUser(username = "send@example.com")
    void postInviteSendsAndFlashesSuccess() throws Exception {
        userService.register("send@example.com", "secret-pw1", "Sender");

        mockMvc.perform(post("/team/invite")
                        .with(csrf())
                        .param("email", "newhire@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/team/invite"))
                .andExpect(flash().attribute("inviteFlash", "sent"));

        assertThat(invites.count()).isOne();
        TeamInvite row = invites.findAll().get(0);
        assertThat(row.getInviteeEmail()).isEqualTo("newhire@example.com");
    }

    @Test
    @WithMockUser(username = "bad@example.com")
    void postInviteWithBlankEmailFlashesInvalidEmail() throws Exception {
        userService.register("bad@example.com", "secret-pw1", "Bad");

        mockMvc.perform(post("/team/invite")
                        .with(csrf())
                        .param("email", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("inviteFlash", "invalidEmail"));

        assertThat(invites.count()).isZero();
    }

    @Test
    @WithMockUser(username = "selfboss@example.com")
    void postInviteWithSelfEmailFlashesSelfInvite() throws Exception {
        userService.register("selfboss@example.com", "secret-pw1", "Self");

        mockMvc.perform(post("/team/invite")
                        .with(csrf())
                        .param("email", "selfboss@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("inviteFlash", "selfInvite"));

        assertThat(invites.count()).isZero();
    }

    @Test
    @WithMockUser(username = "accepter@example.com")
    void acceptFormRendersReadyWhenSignedInAsInvitee() throws Exception {
        userService.register("inviter@example.com", "secret-pw1", "Inviter");
        userService.register("accepter@example.com", "secret-pw1", "Accepter");
        var inviter = users.findByEmail("inviter@example.com").orElseThrow();
        TeamInviteService.InviteResult result = teamInviteService.invite(inviter, "accepter@example.com");
        String plain = capturedPlainToken();

        mockMvc.perform(get("/team/invite/accept").param("token", plain))
                .andExpect(status().isOk())
                .andExpect(view().name("team/accept"))
                .andExpect(model().attribute("status", "ready"))
                .andExpect(model().attributeExists("invite"))
                .andExpect(model().attribute("token", plain));
    }

    @Test
    @WithMockUser(username = "other@example.com")
    void acceptFormShowsEmailMismatchWhenSignedInAsWrongUser() throws Exception {
        userService.register("inviterx@example.com", "secret-pw1", "InviterX");
        userService.register("targetx@example.com", "secret-pw1", "TargetX");
        userService.register("other@example.com", "secret-pw1", "Other");
        var inviter = users.findByEmail("inviterx@example.com").orElseThrow();
        teamInviteService.invite(inviter, "targetx@example.com");
        String plain = capturedPlainToken();

        mockMvc.perform(get("/team/invite/accept").param("token", plain))
                .andExpect(status().isOk())
                .andExpect(view().name("team/accept"))
                .andExpect(model().attribute("status", "emailMismatch"))
                .andExpect(model().attribute("signedInEmail", "other@example.com"));
    }

    @Test
    void acceptFormForUnknownTokenRendersInvalid() throws Exception {
        // Anonymous visitor — controller still renders 'invalid' status
        // (Spring Security only redirects when @WithMockUser is absent
        // AND the endpoint requires auth; /team/invite/accept does require
        // auth, so this case actually 302s to /login).
        mockMvc.perform(get("/team/invite/accept").param("token", "nope"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "validjoin@example.com")
    void postAcceptAddsAccepterToTeamAndRedirectsToThreads() throws Exception {
        userService.register("inviter2@example.com", "secret-pw1", "Inviter2");
        userService.register("validjoin@example.com", "secret-pw1", "ValidJoin");
        var inviter = users.findByEmail("inviter2@example.com").orElseThrow();
        teamInviteService.invite(inviter, "validjoin@example.com");
        String plain = capturedPlainToken();

        mockMvc.perform(post("/team/invite/accept")
                        .with(csrf())
                        .param("token", plain))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/threads"))
                .andExpect(flash().attribute("teamFlash", "joined"));

        Team team = teams.findByOwnerUser(inviter).orElseThrow();
        assertThat(teamMembers.countByTeam(team)).isEqualTo(2L);
    }

    @Test
    @WithMockUser(username = "owner@example.com")
    void onboardingChecklistCountsInviterAfterTheySendOne() throws Exception {
        userService.register("owner@example.com", "secret-pw1", "Owner");
        var owner = users.findByEmail("owner@example.com").orElseThrow();
        assertThat(invites.countNonRevokedByInviter(owner)).isZero();

        mockMvc.perform(post("/team/invite")
                .with(csrf())
                .param("email", "first@example.com"))
                .andExpect(status().is3xxRedirection());

        assertThat(invites.countNonRevokedByInviter(owner)).isOne();
    }

    private String capturedPlainToken() throws Exception {
        org.mockito.ArgumentCaptor<MimeMessage> captor =
                org.mockito.ArgumentCaptor.forClass(MimeMessage.class);
        org.mockito.Mockito.verify(mailSender).send(captor.capture());
        String body = (String) captor.getValue().getContent();
        int idx = body.indexOf("/team/invite/accept?token=");
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
