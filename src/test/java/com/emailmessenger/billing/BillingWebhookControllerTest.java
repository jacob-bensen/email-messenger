package com.emailmessenger.billing;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.Subscription;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.repository.SubscriptionRepository;
import com.emailmessenger.repository.UserRepository;
import com.emailmessenger.service.ReplyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class BillingWebhookControllerTest {

    private static final String WEBHOOK_SECRET = "whsec_test_secret_value";

    @Autowired MockMvc mockMvc;
    @Autowired BillingProperties properties;
    @Autowired UserService userService;
    @Autowired UserRepository users;
    @Autowired SubscriptionRepository subscriptions;

    @MockBean StripeCheckoutGateway checkoutGateway;
    @MockBean ReplyService replyService;
    @MockBean EmailThreadRepository threadRepository;

    private User user;

    @BeforeEach
    void setup() {
        properties.setWebhookSecret(WEBHOOK_SECRET);
        userService.register("hook@example.com", "password1", "Hook");
        user = users.findByEmail("hook@example.com").orElseThrow();
        Subscription pending = new Subscription(user, "cus_hook_test", "incomplete");
        pending.setPlan(Plan.PERSONAL);
        pending.setStripePriceId("price_personal_test");
        subscriptions.save(pending);
    }

    @Test
    void verifiedCheckoutCompletedFlipsLocalSubscriptionToTrialing() throws Exception {
        String payload = """
                {
                  "id": "evt_test_checkout",
                  "type": "checkout.session.completed",
                  "data": {
                    "object": {
                      "id": "cs_test_1",
                      "customer": "cus_hook_test",
                      "subscription": "sub_test_completed"
                    }
                  }
                }
                """;
        String header = sign(payload);

        mockMvc.perform(post("/billing/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", header)
                        .content(payload))
                .andExpect(status().isOk());

        Subscription sub = subscriptions.findByUser(user).orElseThrow();
        assertThat(sub.getStripeSubscriptionId()).isEqualTo("sub_test_completed");
        assertThat(sub.getStatus()).isEqualTo("trialing");
    }

    @Test
    void unverifiedSignatureReturns400AndLeavesStateAlone() throws Exception {
        String payload = """
                {
                  "id": "evt_test_bad",
                  "type": "checkout.session.completed",
                  "data": { "object": { "customer": "cus_hook_test", "subscription": "sub_x" } }
                }
                """;

        mockMvc.perform(post("/billing/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", "t=1700000000,v1=deadbeef")
                        .content(payload))
                .andExpect(status().isBadRequest());

        assertThat(subscriptions.findByUser(user).orElseThrow().getStatus()).isEqualTo("incomplete");
    }

    @Test
    void missingSignatureHeaderReturns400() throws Exception {
        mockMvc.perform(post("/billing/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    private String sign(String payload) throws Exception {
        long timestamp = System.currentTimeMillis() / 1000;
        String signedPayload = timestamp + "." + payload;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String v1 = HexFormat.of().formatHex(mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8)));
        return "t=" + timestamp + ",v1=" + v1;
    }
}
