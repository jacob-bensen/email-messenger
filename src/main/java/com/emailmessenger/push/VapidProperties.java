package com.emailmessenger.push;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * VAPID (RFC 8292) identity for Web Push. The public key is handed to the
 * browser during subscribe() so the push service can verify signed
 * push requests come from us; the private key is used server-side to
 * sign those JWT auth headers when actually sending a push. Both must be
 * a matched EC P-256 pair — encode as url-safe base64 (no padding).
 *
 * <p>Blank keys keep the whole Push feature inert:
 * {@link PushSubscriptionController} refuses to hand out the public key
 * and refuses new subscriptions, so the UI stays in "coming soon" mode.
 */
@ConfigurationProperties("push.vapid")
public class VapidProperties {

    private String publicKey = "";
    private String privateKey = "";
    /**
     * mailto: URI the push service will use to reach us about our sends
     * (per VAPID spec §2, the "sub" claim). Falls back to a generic support
     * address; override once the real support mailbox exists.
     */
    private String subject = "mailto:support@conexusmail.com";

    public String getPublicKey() { return publicKey; }
    public String getPrivateKey() { return privateKey; }
    public String getSubject() { return subject; }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey == null ? "" : publicKey.trim();
    }
    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey == null ? "" : privateKey.trim();
    }
    public void setSubject(String subject) {
        if (subject != null && !subject.isBlank()) {
            this.subject = subject.trim();
        }
    }

    public boolean isEnabled() {
        return !publicKey.isBlank() && !privateKey.isBlank();
    }
}
