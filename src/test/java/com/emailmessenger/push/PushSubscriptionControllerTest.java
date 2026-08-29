package com.emailmessenger.push;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.billing.StripeCheckoutGateway;
import com.emailmessenger.billing.StripePortalGateway;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.PushSubscriptionRepository;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "push.vapid.public-key=BE_TEST_PUBLIC_KEY_URL_SAFE_B64",
        "push.vapid.private-key=BE_TEST_PRIVATE_KEY_URL_SAFE_B64",
        "push.vapid.subject=mailto:test@example.com"
})
class PushSubscriptionControllerTest {

    private static final String ENDPOINT = "https://push.example.com/send/abc123";
    private static final String P256DH = "BExamplePublicKeyBytes";
    private static final String AUTH = "authSecret16Bytes";

    @Autowired MockMvc mockMvc;
    @Autowired UserService userService;
    @Autowired PushSubscriptionRepository subscriptions;

    @MockitoBean JavaMailSender mailSender;
    @MockitoBean StripeCheckoutGateway stripeCheckout;
    @MockitoBean StripePortalGateway stripePortal;
    @MockitoBean ReplyService replyService;

    @BeforeEach
    void stubMimeFactory() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
    }

    @Test
    void vapidPublicKeyRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/account/notifications/push/vapid-public-key"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "vapid@example.com")
    void vapidPublicKeyReturnsConfiguredValue() throws Exception {
        userService.register("vapid@example.com", "password1", null);

        mockMvc.perform(get("/account/notifications/push/vapid-public-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicKey").value("BE_TEST_PUBLIC_KEY_URL_SAFE_B64"));
    }

    @Test
    @WithMockUser(username = "sub@example.com")
    void subscribePersistsSubscription() throws Exception {
        User user = userService.register("sub@example.com", "password1", null);
        String body = "{\"endpoint\":\"" + ENDPOINT + "\","
                + "\"p256dhKey\":\"" + P256DH + "\","
                + "\"authKey\":\"" + AUTH + "\"}";

        mockMvc.perform(post("/account/notifications/push/subscribe")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ok").value(true));

        assertThat(subscriptions.findByUserAndEndpoint(user, ENDPOINT)).isPresent()
                .get().satisfies(sub -> {
                    assertThat(sub.getP256dhKey()).isEqualTo(P256DH);
                    assertThat(sub.getAuthKey()).isEqualTo(AUTH);
                });
    }

    @Test
    @WithMockUser(username = "reup@example.com")
    void resubscribingWithSameEndpointUpsertsInsteadOfDuplicating() throws Exception {
        User user = userService.register("reup@example.com", "password1", null);
        String initial = "{\"endpoint\":\"" + ENDPOINT + "\","
                + "\"p256dhKey\":\"OldKey\",\"authKey\":\"OldAuth\"}";
        String rotated = "{\"endpoint\":\"" + ENDPOINT + "\","
                + "\"p256dhKey\":\"NewKey\",\"authKey\":\"NewAuth\"}";

        mockMvc.perform(post("/account/notifications/push/subscribe")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(initial))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/account/notifications/push/subscribe")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rotated))
                .andExpect(status().isCreated());

        assertThat(subscriptions.countByUser(user)).isEqualTo(1);
        assertThat(subscriptions.findByUserAndEndpoint(user, ENDPOINT))
                .get().satisfies(sub -> {
                    assertThat(sub.getP256dhKey()).isEqualTo("NewKey");
                    assertThat(sub.getAuthKey()).isEqualTo("NewAuth");
                });
    }

    @Test
    @WithMockUser(username = "gone@example.com")
    void unsubscribeRemovesTheStoredSubscription() throws Exception {
        User user = userService.register("gone@example.com", "password1", null);
        String body = "{\"endpoint\":\"" + ENDPOINT + "\","
                + "\"p256dhKey\":\"" + P256DH + "\",\"authKey\":\"" + AUTH + "\"}";
        mockMvc.perform(post("/account/notifications/push/subscribe")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        String unsub = "{\"endpoint\":\"" + ENDPOINT + "\"}";
        mockMvc.perform(delete("/account/notifications/push/subscribe")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unsub))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.removed").value(true));

        assertThat(subscriptions.findByUserAndEndpoint(user, ENDPOINT)).isEmpty();
    }

    @Test
    @WithMockUser(username = "invalid@example.com")
    void subscribeRejectsMissingFields() throws Exception {
        userService.register("invalid@example.com", "password1", null);

        mockMvc.perform(post("/account/notifications/push/subscribe")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"endpoint\":\"\",\"p256dhKey\":\"\",\"authKey\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
