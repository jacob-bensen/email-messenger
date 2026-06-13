package com.emailmessenger.email;

import jakarta.mail.internet.MimeMessage;

import java.util.List;

/**
 * Thin seam over Jakarta Mail's IMAP store so the connect-and-sync flow
 * can be unit-tested without a real IMAP server. Implementations open
 * INBOX, fetch the latest {@code limit} messages, and close the store.
 */
public interface ImapClient {

    /**
     * Opens an IMAP connection with the given credentials and closes it
     * immediately. Throws {@link ImapConnectionException} on any failure
     * (network, auth, TLS). Used as a credential-validation step before
     * persisting a {@code MailAccount}.
     */
    void verifyConnection(String host, int port, boolean ssl,
                          String username, String password);

    /**
     * Fetches the {@code limit} most recently received messages from INBOX,
     * newest first. Returns an empty list if the folder is empty. Throws
     * {@link ImapConnectionException} on any failure.
     */
    List<MimeMessage> fetchRecentInbox(String host, int port, boolean ssl,
                                       String username, String password, int limit);

    /**
     * Incremental sync: fetches every INBOX message whose IMAP UID is
     * strictly greater than {@code lastSeenUid}. When {@code lastSeenUid}
     * is {@code null}, no messages are returned but the highest UID in
     * INBOX is reported back so subsequent polls can pick up from there
     * without re-importing the initial-sync window.
     *
     * @return new messages plus the highest UID observed; {@code newLastUid}
     *         is {@code null} only when the folder is empty and there is
     *         no prior baseline.
     * @throws ImapConnectionException on any IMAP failure
     */
    IncrementalFetch fetchSinceUid(String host, int port, boolean ssl,
                                   String username, String password, Long lastSeenUid);

    record IncrementalFetch(List<MimeMessage> messages, Long newLastUid) {}
}
