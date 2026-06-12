package com.emailmessenger.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

/**
 * Symmetric encryption for IMAP passwords at rest. Backed by Spring
 * Security's AES-GCM {@code Encryptors.stronger}. The key + salt live
 * in environment variables in production; dev falls back to a fixed
 * pair so the app boots out-of-the-box. A warning is logged on the
 * fallback so operators don't accidentally ship the dev keys.
 */
@Component
public class CredentialEncryptor {

    private static final Logger log = LoggerFactory.getLogger(CredentialEncryptor.class);

    // Hex-encoded 8-byte salt. Fixed-value dev fallback; override in prod.
    static final String DEV_FALLBACK_SALT = "5c0744940b5c369b";
    static final String DEV_FALLBACK_PASSWORD = "mailim-dev-credential-key";

    private final TextEncryptor encryptor;

    CredentialEncryptor(@Value("${mailbox.encryption.password:}") String password,
                        @Value("${mailbox.encryption.salt:}") String salt) {
        String effectivePassword = password.isBlank() ? DEV_FALLBACK_PASSWORD : password;
        String effectiveSalt = salt.isBlank() ? DEV_FALLBACK_SALT : salt;
        if (password.isBlank() || salt.isBlank()) {
            log.warn("Using dev fallback IMAP credential encryption key — "
                    + "set MAILBOX_ENCRYPTION_PASSWORD and MAILBOX_ENCRYPTION_SALT in production.");
        }
        this.encryptor = Encryptors.delux(effectivePassword, effectiveSalt);
    }

    public String encrypt(String plaintext) {
        return encryptor.encrypt(plaintext);
    }

    public String decrypt(String ciphertext) {
        return encryptor.decrypt(ciphertext);
    }
}
