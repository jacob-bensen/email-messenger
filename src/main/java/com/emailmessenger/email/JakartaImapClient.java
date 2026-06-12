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
import java.util.Properties;

@Component
class JakartaImapClient implements ImapClient {

    private static final Logger log = LoggerFactory.getLogger(JakartaImapClient.class);

    @Override
    public void verifyConnection(String host, int port, boolean ssl,
                                 String username, String password) {
        Store store = null;
        try {
            store = openStore(host, port, ssl, username, password);
        } catch (MessagingException e) {
            log.info("IMAP verify failed for {}@{}: {}", username, host, e.getMessage());
            throw new ImapConnectionException(humanReadable(e), e);
        } finally {
            closeQuietly(store);
        }
    }

    @Override
    public List<MimeMessage> fetchRecentInbox(String host, int port, boolean ssl,
                                              String username, String password, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        Store store = null;
        Folder inbox = null;
        try {
            store = openStore(host, port, ssl, username, password);
            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);
            int total = inbox.getMessageCount();
            if (total == 0) {
                return List.of();
            }
            int start = Math.max(1, total - limit + 1);
            Message[] fetched = inbox.getMessages(start, total);
            // Copy out as MimeMessage so we can close the folder/store without
            // losing access to the message bodies (Jakarta Mail lazily streams).
            List<MimeMessage> out = new ArrayList<>(fetched.length);
            for (Message m : fetched) {
                if (m instanceof MimeMessage mime) {
                    out.add(new MimeMessage(mime));
                }
            }
            return out;
        } catch (MessagingException e) {
            log.info("IMAP fetch failed for {}@{}: {}", username, host, e.getMessage());
            throw new ImapConnectionException(humanReadable(e), e);
        } finally {
            closeQuietly(inbox);
            closeQuietly(store);
        }
    }

    @Override
    public IncrementalFetch fetchSinceUid(String host, int port, boolean ssl,
                                          String username, String password, Long lastSeenUid) {
        Store store = null;
        Folder inbox = null;
        try {
            store = openStore(host, port, ssl, username, password);
            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);
            if (!(inbox instanceof UIDFolder uidFolder)) {
                throw new ImapConnectionException(
                        "Server does not support IMAP UIDs (required for incremental sync).", null);
            }
            int total = inbox.getMessageCount();
            if (lastSeenUid == null) {
                if (total == 0) {
                    return new IncrementalFetch(List.of(), null);
                }
                long baseline = uidFolder.getUID(inbox.getMessage(total));
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
        } catch (MessagingException e) {
            log.info("IMAP incremental fetch failed for {}@{}: {}", username, host, e.getMessage());
            throw new ImapConnectionException(humanReadable(e), e);
        } finally {
            closeQuietly(inbox);
            closeQuietly(store);
        }
    }

    private Store openStore(String host, int port, boolean ssl,
                            String username, String password) throws MessagingException {
        Properties props = new Properties();
        String protocol = ssl ? "imaps" : "imap";
        props.put("mail.store.protocol", protocol);
        props.put("mail." + protocol + ".host", host);
        props.put("mail." + protocol + ".port", String.valueOf(port));
        props.put("mail." + protocol + ".connectiontimeout", "10000");
        props.put("mail." + protocol + ".timeout", "15000");
        if (!ssl) {
            props.put("mail.imap.starttls.enable", "true");
        }
        Session session = Session.getInstance(props);
        Store store = session.getStore(protocol);
        store.connect(host, port, username, password);
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
