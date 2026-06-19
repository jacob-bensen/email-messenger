package com.emailmessenger.email;

import jakarta.mail.internet.MimeMessage;

import java.util.List;

/**
 * Thin seam over Jakarta Mail's IMAP store so the connect-and-sync flow
 * can be unit-tested without a real IMAP server. Implementations open
 * INBOX, fetch the requested messages, and close the store. Authentication
 * (password vs XOAUTH2) is carried by {@link ImapCredentials}.
 */
public interface ImapClient {

    /**
     * Opens an IMAP connection with the given credentials and closes it
     * immediately. Throws {@link ImapConnectionException} on any failure
     * (network, auth, TLS). Used as a credential-validation step before
     * persisting a {@code MailAccount}.
     */
    void verifyConnection(ImapCredentials credentials);

    /**
     * Fetches the {@code limit} most recently received messages from INBOX,
     * newest first. Returns an empty list if the folder is empty. Throws
     * {@link ImapConnectionException} on any failure.
     */
    List<MimeMessage> fetchRecentInbox(ImapCredentials credentials, int limit);

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
    IncrementalFetch fetchSinceUid(ImapCredentials credentials, Long lastSeenUid);

    /**
     * Like {@link #fetchRecentInbox} but for the mailbox's Sent folder (used to
     * backfill the owner's sent history as outbound messages). Returns an empty
     * list if no Sent folder can be located on the server.
     */
    List<MimeMessage> fetchRecentSent(ImapCredentials credentials, int limit);

    /**
     * Like {@link #fetchSinceUid} but against the Sent folder. When no Sent
     * folder exists the supplied cursor is echoed back unchanged so nothing is
     * imported and the cursor doesn't advance.
     */
    IncrementalFetch fetchSentSinceUid(ImapCredentials credentials, Long lastSeenSentUid);

    record IncrementalFetch(List<MimeMessage> messages, Long newLastUid) {}
}
