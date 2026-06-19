package com.emailmessenger.email;

import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.UIDFolder;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

@Component
class JakartaImapClient implements ImapClient {

    private static final Logger log = LoggerFactory.getLogger(JakartaImapClient.class);

    // Common Sent-folder names across providers (Gmail, Outlook/Office365,
    // generic, Dovecot/cPanel namespace). Tried in order before a name scan.
    private static final String[] SENT_FOLDER_CANDIDATES = {
            "[Gmail]/Sent Mail", "Sent Items", "Sent", "Sent Mail", "INBOX.Sent"
    };

    @Override
    public void verifyConnection(ImapCredentials credentials) {
        Store store = null;
        try {
            store = openStore(credentials);
        } catch (MessagingException e) {
            log.info("IMAP verify failed for {}@{}: {}",
                    credentials.username(), credentials.host(), e.getMessage());
            throw new ImapConnectionException(humanReadable(e), e);
        } finally {
            closeQuietly(store);
        }
    }

    @Override
    public List<MimeMessage> fetchRecentInbox(ImapCredentials credentials, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        Store store = null;
        Folder inbox = null;
        try {
            store = openStore(credentials);
            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);
            return fetchRecent(inbox, limit);
        } catch (MessagingException e) {
            log.info("IMAP fetch failed for {}@{}: {}",
                    credentials.username(), credentials.host(), e.getMessage());
            throw new ImapConnectionException(humanReadable(e), e);
        } finally {
            closeQuietly(inbox);
            closeQuietly(store);
        }
    }

    @Override
    public IncrementalFetch fetchSinceUid(ImapCredentials credentials, Long lastSeenUid) {
        Store store = null;
        Folder inbox = null;
        try {
            store = openStore(credentials);
            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);
            return fetchSince(inbox, lastSeenUid);
        } catch (MessagingException e) {
            log.info("IMAP incremental fetch failed for {}@{}: {}",
                    credentials.username(), credentials.host(), e.getMessage());
            throw new ImapConnectionException(humanReadable(e), e);
        } finally {
            closeQuietly(inbox);
            closeQuietly(store);
        }
    }

    @Override
    public List<MimeMessage> fetchRecentSent(ImapCredentials credentials, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        Store store = null;
        Folder sent = null;
        try {
            store = openStore(credentials);
            sent = openSentFolder(store);
            if (sent == null) {
                log.info("No Sent folder found for {}@{}; skipping sent backfill.",
                        credentials.username(), credentials.host());
                return List.of();
            }
            return fetchRecent(sent, limit);
        } catch (MessagingException e) {
            log.info("IMAP sent fetch failed for {}@{}: {}",
                    credentials.username(), credentials.host(), e.getMessage());
            throw new ImapConnectionException(humanReadable(e), e);
        } finally {
            closeQuietly(sent);
            closeQuietly(store);
        }
    }

    @Override
    public IncrementalFetch fetchSentSinceUid(ImapCredentials credentials, Long lastSeenSentUid) {
        Store store = null;
        Folder sent = null;
        try {
            store = openStore(credentials);
            sent = openSentFolder(store);
            if (sent == null) {
                return new IncrementalFetch(List.of(), lastSeenSentUid);
            }
            return fetchSince(sent, lastSeenSentUid);
        } catch (MessagingException e) {
            log.info("IMAP sent incremental fetch failed for {}@{}: {}",
                    credentials.username(), credentials.host(), e.getMessage());
            throw new ImapConnectionException(humanReadable(e), e);
        } finally {
            closeQuietly(sent);
            closeQuietly(store);
        }
    }

    /** Fetches the {@code limit} most recent messages from an already-open folder. */
    private List<MimeMessage> fetchRecent(Folder folder, int limit) throws MessagingException {
        int total = folder.getMessageCount();
        if (total == 0) {
            return List.of();
        }
        int start = Math.max(1, total - limit + 1);
        Message[] fetched = folder.getMessages(start, total);
        // Copy out as MimeMessage so the folder/store can close without losing
        // body access (Jakarta Mail lazily streams).
        List<MimeMessage> out = new ArrayList<>(fetched.length);
        for (Message m : fetched) {
            if (m instanceof MimeMessage mime) {
                out.add(new MimeMessage(mime));
            }
        }
        return out;
    }

    /** Incremental fetch (UID strictly greater than the cursor) from an open folder. */
    private IncrementalFetch fetchSince(Folder folder, Long lastSeenUid) throws MessagingException {
        if (!(folder instanceof UIDFolder uidFolder)) {
            throw new ImapConnectionException(
                    "Server does not support IMAP UIDs (required for incremental sync).", null);
        }
        int total = folder.getMessageCount();
        if (lastSeenUid == null) {
            if (total == 0) {
                return new IncrementalFetch(List.of(), null);
            }
            long baseline = uidFolder.getUID(folder.getMessage(total));
            return new IncrementalFetch(List.of(), baseline);
        }
        if (total == 0) {
            return new IncrementalFetch(List.of(), lastSeenUid);
        }
        Message[] msgs = uidFolder.getMessagesByUID(lastSeenUid + 1L, UIDFolder.LASTUID);
        List<MimeMessage> out = new ArrayList<>(msgs.length);
        long newLast = lastSeenUid;
        for (Message m : msgs) {
            if (m == null) continue;
            long uid = uidFolder.getUID(m);
            // Some servers include the boundary UID; filter strictly above lastSeen.
            if (uid <= lastSeenUid) continue;
            if (m instanceof MimeMessage mime) {
                out.add(new MimeMessage(mime));
            }
            if (uid > newLast) newLast = uid;
        }
        return new IncrementalFetch(out, newLast);
    }

    /**
     * Locates and opens (READ_ONLY) the mailbox's Sent folder, or returns
     * {@code null} if none can be found. Tries well-known names first, then
     * scans for any message-holding folder whose name contains "sent".
     */
    private Folder openSentFolder(Store store) throws MessagingException {
        for (String name : SENT_FOLDER_CANDIDATES) {
            Folder f = store.getFolder(name);
            if (f.exists()) {
                f.open(Folder.READ_ONLY);
                return f;
            }
        }
        for (Folder f : store.getDefaultFolder().list("*")) {
            String name = f.getName();
            if (name != null && name.toLowerCase(Locale.ROOT).contains("sent")
                    && (f.getType() & Folder.HOLDS_MESSAGES) != 0) {
                f.open(Folder.READ_ONLY);
                return f;
            }
        }
        return null;
    }

    private Store openStore(ImapCredentials credentials) throws MessagingException {
        Properties props = new Properties();
        String protocol = credentials.ssl() ? "imaps" : "imap";
        props.put("mail.store.protocol", protocol);
        props.put("mail." + protocol + ".host", credentials.host());
        props.put("mail." + protocol + ".port", String.valueOf(credentials.port()));
        props.put("mail." + protocol + ".connectiontimeout", "10000");
        props.put("mail." + protocol + ".timeout", "15000");
        if (!credentials.ssl()) {
            props.put("mail.imap.starttls.enable", "true");
        }
        if (credentials.mechanism() == ImapCredentials.Mechanism.XOAUTH2) {
            // Jakarta Mail's built-in XOAUTH2 support: restrict the auth
            // mechanism list and pass the OAuth access token where the
            // password normally goes. SASL is left off so the client uses
            // its own XOAUTH2 implementation rather than a JAAS provider.
            props.put("mail." + protocol + ".auth.mechanisms", "XOAUTH2");
            props.put("mail." + protocol + ".sasl.enable", "false");
        }
        Session session = Session.getInstance(props);
        Store store = session.getStore(protocol);
        store.connect(credentials.host(), credentials.port(),
                credentials.username(), credentials.secret());
        return store;
    }

    private static String humanReadable(MessagingException e) {
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) {
            return "Could not connect to the IMAP server.";
        }
        // Trim multi-line server banners to something fit for a form-error label.
        int newline = msg.indexOf('\n');
        return newline > 0 ? msg.substring(0, newline) : msg;
    }

    private static void closeQuietly(Folder f) {
        if (f != null && f.isOpen()) {
            try { f.close(false); } catch (MessagingException ignored) { /* best-effort */ }
        }
    }

    private static void closeQuietly(Store s) {
        if (s != null && s.isConnected()) {
            try { s.close(); } catch (MessagingException ignored) { /* best-effort */ }
        }
    }
}
