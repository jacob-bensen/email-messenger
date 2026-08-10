package com.emailmessenger.email;

/**
 * Seam over IMAP IDLE (RFC 2177) so the idle loop can be unit-tested without
 * a real server. One connection watches one account's INBOX.
 */
interface ImapIdleClient {

    /**
     * Opens a connection and enters a state ready to IDLE on INBOX. Throws
     * {@link ImapConnectionException} on any failure, including a server
     * without the IDLE capability (such accounts stay on plain polling).
     */
    IdleConnection open(ImapCredentials credentials);

    interface IdleConnection extends AutoCloseable {

        /**
         * Blocks in IMAP IDLE until something happens on INBOX.
         *
         * @return {@code true} when new messages arrived; {@code false} when
         *         the idle window elapsed quietly — the caller should
         *         reconnect with fresh credentials (OAuth access tokens
         *         outlive a window but not many of them)
         * @throws ImapConnectionException when the connection is no longer
         *         usable
         */
        boolean awaitNewMail();

        /** Idempotent; safe to call from another thread to abort an idle. */
        @Override
        void close();
    }
}
