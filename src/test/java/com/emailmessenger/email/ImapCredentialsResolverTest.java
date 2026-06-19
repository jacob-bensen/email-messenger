package com.emailmessenger.email;

import com.emailmessenger.domain.AuthType;
import com.emailmessenger.domain.MailAccount;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImapCredentialsResolverTest {

    private final CredentialEncryptor encryptor = mock(CredentialEncryptor.class);
    private final GmailOAuthClient gmailOAuthClient = mock(GmailOAuthClient.class);
    private final ImapCredentialsResolver resolver =
            new ImapCredentialsResolver(encryptor, gmailOAuthClient);

    @Test
    void passwordAccountResolvesToDecryptedPasswordWithoutTouchingOAuth() {
        MailAccount account = new MailAccount(null, "imap.example.com", 993, true,
                "user@example.com", "cipher");
        when(encryptor.decrypt("cipher")).thenReturn("plain-pw");

        ImapCredentials creds = resolver.resolve(account);

        assertThat(creds).isEqualTo(ImapCredentials.password(
                "imap.example.com", 993, true, "user@example.com", "plain-pw"));
        verify(gmailOAuthClient, never()).refreshAccessToken(eq("plain-pw"));
    }

    @Test
    void xoauth2AccountRefreshesRefreshTokenIntoAccessTokenCreds() {
        MailAccount account = new MailAccount(null, "imap.gmail.com", 993, true,
                "user@gmail.com", "cipher-rt", AuthType.XOAUTH2);
        when(encryptor.decrypt("cipher-rt")).thenReturn("refresh-token");
        when(gmailOAuthClient.refreshAccessToken("refresh-token")).thenReturn("fresh-access-token");

        ImapCredentials creds = resolver.resolve(account);

        assertThat(creds).isEqualTo(ImapCredentials.xoauth2(
                "imap.gmail.com", 993, true, "user@gmail.com", "fresh-access-token"));
    }

    @Test
    void expiredRefreshTokenSurfacesAsImapConnectionException() {
        MailAccount account = new MailAccount(null, "imap.gmail.com", 993, true,
                "user@gmail.com", "cipher-rt", AuthType.XOAUTH2);
        when(encryptor.decrypt("cipher-rt")).thenReturn("refresh-token");
        when(gmailOAuthClient.refreshAccessToken("refresh-token"))
                .thenThrow(new GmailOAuthException("invalid_grant", null));

        assertThatThrownBy(() -> resolver.resolve(account))
                .isInstanceOf(ImapConnectionException.class)
                .hasMessageContaining("reconnect");
    }

    @Test
    void undecryptableSecretSurfacesAsImapConnectionException() {
        MailAccount account = new MailAccount(null, "imap.example.com", 993, true,
                "user@example.com", "garbage");
        when(encryptor.decrypt("garbage")).thenThrow(new IllegalStateException("bad cipher"));

        assertThatThrownBy(() -> resolver.resolve(account))
                .isInstanceOf(ImapConnectionException.class)
                .hasMessageContaining("decrypt");
    }
}
