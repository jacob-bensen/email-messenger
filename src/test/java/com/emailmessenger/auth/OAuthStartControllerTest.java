package com.emailmessenger.auth;

import com.emailmessenger.billing.BillingService;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.service.ReplyService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /auth/google/start captures plan/billing/utm_source into session before
 * handing off to Spring Security's OAuth2 authorization filter so the
 * Google round-trip preserves the visitor's checkout intent.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OAuthStartControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean ReplyService replyService;
    @MockitoBean EmailThreadRepository threadRepository;
    @MockitoBean BillingService billingService;

    @Test
    void storesPlanBillingUtmIntoSessionAndRedirectsToSpringSecurityAuthorization() throws Exception {
        HttpSession session = mockMvc.perform(get("/auth/google/start")
                        .param("plan", "personal")
                        .param("billing", "annual")
                        .param("utm_source", "producthunt"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/oauth2/authorization/google"))
                .andReturn().getRequest().getSession(false);

        assertThat(session).isNotNull();
        assertThat(session.getAttribute("conexusmail.oauth.plan")).isEqualTo("personal");
        assertThat(session.getAttribute("conexusmail.oauth.billing")).isEqualTo("annual");
        assertThat(session.getAttribute("conexusmail.oauth.utm_source")).isEqualTo("producthunt");
    }

    @Test
    void missingParamsLeaveSessionAttributesUnsetAndStillRedirect() throws Exception {
        HttpSession session = mockMvc.perform(get("/auth/google/start"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/oauth2/authorization/google"))
                .andReturn().getRequest().getSession(false);

        assertThat(session).isNotNull();
        assertThat(session.getAttribute("conexusmail.oauth.plan")).isNull();
        assertThat(session.getAttribute("conexusmail.oauth.billing")).isNull();
        assertThat(session.getAttribute("conexusmail.oauth.utm_source")).isNull();
    }

    @Test
    void unknownPlanIsDroppedSoTamperedQueryStringIsHarmless() throws Exception {
        HttpSession session = mockMvc.perform(get("/auth/google/start")
                        .param("plan", "MYSTERYTIER"))
                .andExpect(status().is3xxRedirection())
                .andReturn().getRequest().getSession(false);

        assertThat(session).isNotNull();
        assertThat(session.getAttribute("conexusmail.oauth.plan")).isNull();
    }

    @Test
    void unknownBillingDegradesToMonthly() throws Exception {
        HttpSession session = mockMvc.perform(get("/auth/google/start")
                        .param("plan", "personal")
                        .param("billing", "quarterly"))
                .andExpect(status().is3xxRedirection())
                .andReturn().getRequest().getSession(false);

        assertThat(session).isNotNull();
        assertThat(session.getAttribute("conexusmail.oauth.plan")).isEqualTo("personal");
        assertThat(session.getAttribute("conexusmail.oauth.billing")).isEqualTo("monthly");
    }

    @Test
    void overlongUtmSourceIsClampedTo64Chars() throws Exception {
        String overlong = "x".repeat(200);

        HttpSession session = mockMvc.perform(get("/auth/google/start")
                        .param("utm_source", overlong))
                .andExpect(status().is3xxRedirection())
                .andReturn().getRequest().getSession(false);

        assertThat(session).isNotNull();
        String stored = (String) session.getAttribute("conexusmail.oauth.utm_source");
        assertThat(stored).hasSize(64);
    }

    @Test
    void freshPlanClearsAnyStaleIntentFromEarlierStart() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("conexusmail.oauth.plan", "team");
        session.setAttribute("conexusmail.oauth.billing", "annual");
        session.setAttribute("conexusmail.oauth.utm_source", "twitter");

        mockMvc.perform(get("/auth/google/start").session(session))
                .andExpect(status().is3xxRedirection());

        assertThat(session.getAttribute("conexusmail.oauth.plan")).isNull();
        assertThat(session.getAttribute("conexusmail.oauth.billing")).isNull();
        assertThat(session.getAttribute("conexusmail.oauth.utm_source")).isNull();
    }
}
