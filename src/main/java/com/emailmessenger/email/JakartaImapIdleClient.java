package com.emailmessenger.email;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import jakarta.mail.Folder;
import jakarta.mail.MessagingException;
import jakarta.mail.Store;
import jakarta.mail.event.MessageCountAdapter;
import jakarta.mail.event.MessageCountEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
class JakartaImapIdleClient implements ImapIdleClient {

    private static final Logger log = LoggerFactory.getLogger(JakartaImapIdleClient.class);

    /**
     * Socket read timeout while idling. RFC 2177 asks clients to re-issue
     * IDLE at least every 29 minutes; a quiet window ends in a read timeout,
     * which {@link Connection#awaitNewMail()} reports as "reconnect" — that
     * also refreshes short-lived OAuth access tokens.
     */
    static final long IDLE_READ_TIMEOUT_MS = 25 * 60 * 1000L;

    @Override
    public IdleConnection open(ImapCredentials credentials) {
        Store store = null;
        try {
            store = JakartaImapClient.openStore(credentials, IDLE_READ_TIMEOUT_MS);
            if (!(store instanceof IMAPStore imapStore) || !imapStore.hasCapability("IDLE")) {
                throw new ImapConnectionException(
                        "Server does not advertise the IMAP IDLE capability.", null);
            }
            IMAPFolder inbox = (IMAPFolder) store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);
            return new Connection(store, inbox);
        } catch (MessagingException e) {
            closeQuietly(store);
            log.info("IMAP IDLE connect failed for {}@{}: {}",
                    credentials.username(), credentials.host(), e.getMessage());
            throw new ImapConnectionException(e.getMessage(), e);
        } catch (ImapConnectionException e) {
            closeQuietly(store);
            throw e;
        }
    }

    private static final class Connection implements IdleConnection {

        private final Store store;
        private final IMAPFolder inbox;
        private final AtomicBoolean newMail = new AtomicBoolean();

        private Connection(Store store, IMAPFolder inbox) {
            this.store = store;
            this.inbox = inbox;
            inbox.addMessageCountListener(new MessageCountAdapter() {
                @Override
                public void messagesAdded(MessageCountEvent e) {
                    newMail.set(true);
                }
            });
        }

        @Override
        public boolean awaitNewMail() {
            newMail.set(false);
            // idle() returns on ANY unsolicited response (flag changes,
            // expunges, ...) — loop until it was actually new mail, so the
            // caller doesn't tear the connection down for noise.
            while (!newMail.get()) {
                try {
                    inbox.idle();
                } catch (MessagingException e) {
                    if (causedByReadTimeout(e)) {
                        return false; // quiet window elapsed — cycle the connection
                    }
                    throw new ImapConnectionException(e.getMessage(), e);
                }
            }
            return true;
        }

        @Override
        public void close() {
            if (inbox.isOpen()) {
                try { inbox.close(false); } catch (MessagingException ignored) { /* best-effort */ }
            }
            closeQuietly(store);
        }

        private static boolean causedByReadTimeout(Throwable t) {
            for (Throwable cur = t; cur != null; cur = cur.getCause()) {
                if (cur instanceof SocketTimeoutException) {
                    return true;
                }
                // com.sun.mail folds the socket timeout into an IOException
                // message on some paths; match defensively.
                if (cur instanceof IOException io && io.getMessage() != null
                        && io.getMessage().toLowerCase().contains("timeout")) {
                    return true;
                }
            }
            return false;
        }
    }

    private static void closeQuietly(Store store) {
        if (store != null && store.isConnected()) {
            try { store.close(); } catch (MessagingException ignored) { /* best-effort */ }
        }
    }
}
