package com.emailmessenger.email;

/** Raised when a Google OAuth token call (code exchange, refresh, userinfo) fails. */
public class GmailOAuthException extends RuntimeException {
    public GmailOAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
