package com.emailmessenger.email;

/**
 * Raised when an IMAP connection attempt fails — typically because the
 * host is unreachable, the credentials are wrong, or the server rejects
 * the TLS handshake. The message is end-user-safe; the controller renders
 * it on the connect form so the user can correct their input.
 */
public class ImapConnectionException extends RuntimeException {

    public ImapConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
