package com.emailmessenger.billing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
class BillingWebhookController {

    private static final Logger log = LoggerFactory.getLogger(BillingWebhookController.class);

    private final StripeWebhookGateway gateway;
    private final BillingService billingService;

    BillingWebhookController(StripeWebhookGateway gateway, BillingService billingService) {
        this.gateway = gateway;
        this.billingService = billingService;
    }

    @PostMapping(path = "/billing/webhook")
    ResponseEntity<String> handle(@RequestBody String payload,
                                  @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        if (signature == null || signature.isBlank()) {
            return ResponseEntity.badRequest().body("missing Stripe-Signature header");
        }
        StripeEvent event;
        try {
            event = gateway.parse(payload, signature);
        } catch (InvalidStripeSignatureException e) {
            log.warn("Rejected Stripe webhook with bad signature");
            return ResponseEntity.badRequest().body("invalid signature");
        } catch (BillingException e) {
            log.error("Stripe webhook misconfigured: {}", e.getMessage());
            return ResponseEntity.status(503).body("webhook not configured");
        }
        try {
            billingService.applyStripeEvent(event);
        } catch (RuntimeException e) {
            log.error("Failed applying Stripe event {} ({})", event.id(), event.type(), e);
            // Stripe retries on non-2xx; return 500 so a transient failure gets retried.
            return ResponseEntity.status(500).body("error");
        }
        return ResponseEntity.ok("ok");
    }
}
