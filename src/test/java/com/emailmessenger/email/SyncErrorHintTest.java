package com.emailmessenger.email;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SyncErrorHintTest {

    @Test
    void hintIsNullWhenNoError() {
        assertThat(SyncErrorHint.forError(null)).isNull();
    }

    @Test
    void hintForExpiredGoogleAuthorizationPointsAtReconnect() {
        assertThat(SyncErrorHint.forError("Google sign-in expired; reconnect the mailbox with Google."))
                .contains("Use Reconnect");
    }

    @Test
    void hintCallsOutAuthenticationFailures() {
        assertThat(SyncErrorHint.forError("AUTHENTICATE failed"))
                .contains("app password");
        assertThat(SyncErrorHint.forError("Invalid login or password"))
                .contains("app password");
    }

    @Test
    void hintForDecryptFailureSaysReconnectNotNewPassword() {
        String hint = SyncErrorHint.forError("Could not decrypt stored credentials");
        assertThat(hint).contains("Reconnect the mailbox");
        assertThat(hint).doesNotContain("app password");
    }

    @Test
    void hintCallsOutConnectivityFailures() {
        assertThat(SyncErrorHint.forError("Connection refused: imap.example.com"))
                .contains("mail server");
        assertThat(SyncErrorHint.forError("Read timed out"))
                .contains("didn't respond");
    }

    @Test
    void hintFallsBackToGenericRetryAdvice() {
        assertThat(SyncErrorHint.forError("Quota exceeded"))
                .contains("Sync now");
    }
}
