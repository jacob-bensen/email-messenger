package com.emailmessenger.email;

import com.emailmessenger.domain.AuthType;
import com.emailmessenger.domain.MailAccount;
import org.springframework.stereotype.Component;

/**
 * Turns a persisted {@link MailAccount} into live {@link ImapCredentials}.
 * For PASSWORD accounts that is just decryption; for XOAUTH2 accounts the
 * stored secret is a refresh token, which is exchanged for a short-lived
 * access token on every call (access tokens expire in ~1h, far shorter than
 * the poll cadence, so caching buys little and risks serving a stale token).
 *
 * <p>Any failure — undecryptable secret or a rejected refresh — surfaces as
 * an {@link ImapConnectionException} so callers handle it on the same path
 * as a normal IMAP login failure.
 */
@Component
class ImapCredentialsResolver {

    private final CredentialEncryptor encryptor;
    private final GmailOAuthClient gmailOAuthClient;

    ImapCredentialsResolver(CredentialEncryptor encryptor, GmailOAuthClient gmailOAuthClient) {
        this.encryptor = encryptor;
        this.gmailOAuthClient = gmailOAuthClient;
    }

    ImapCredentials resolve(MailAccount account) {
        String secret;
        try {
            secret = encryptor.decrypt(account.getPasswordCiphertext());
        } catch (Exception e) {
            throw new ImapConnectionException("Could not decrypt stored credentials", e);
        }
        if (account.getAuthType() == AuthType.XOAUTH2) {
            String accessToken;
            try {
                accessToken = gmailOAuthClient.refreshAccessToken(secret);
            } catch (GmailOAuthException e) {
                throw new ImapConnectionException(
                        "Google sign-in expired; reconnect the mailbox with Google.", e);
            }
            return ImapCredentials.xoauth2(account.getHost(), account.getPort(),
                    account.isSsl(), account.getUsername(), accessToken);
        }
        return ImapCredentials.password(account.getHost(), account.getPort(),
                account.isSsl(), account.getUsername(), secret);
    }
}
