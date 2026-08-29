package com.emailmessenger.push;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.billing.StripeCheckoutGateway;
import com.emailmessenger.billing.StripePortalGateway;
import com.emailmessenger.service.ReplyService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * When VAPID keys are blank (the default / dev configuration), the push
 * endpoints must degrade to 404 so the notifications page falls back to
 * its "coming soon" UI rather than exposing a switch that can't work.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PushSubscriptionUnconfiguredTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserService userService;

    @MockitoBean JavaMailSender mailSender;
    @MockitoBean StripeCheckoutGateway stripeCheckout;
    @MockitoBean StripePortalGateway stripePortal;
    @MockitoBean ReplyService replyService;

    @BeforeEach
    void stubMimeFactory() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
    }

    @Test
    @WithMockUser(username = "novapid@example.com")
    void vapidPublicKeyIs404WhenNotConfigured() throws Exception {
        userService.register("novapid@example.com", "password1", null);
        mockMvc.perform(get("/account/notifications/push/vapid-public-key"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "novapidsub@example.com")
    void subscribeIs404WhenNotConfigured() throws Exception {
        userService.register("novapidsub@example.com", "password1", null);
        mockMvc.perform(post("/account/notifications/push/subscribe")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"endpoint\":\"https://x\",\"p256dhKey\":\"k\",\"authKey\":\"a\"}"))
                .andExpect(status().isNotFound());
    }
}
