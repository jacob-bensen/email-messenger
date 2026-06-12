package com.emailmessenger.email;

import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.search.FlagTerm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
@ConditionalOnProperty(name = "app.imap.polling.enabled", havingValue = "true")
class ImapPollingJob {

    private static final Logger log = LoggerFactory.getLogger(ImapPollingJob.class);

    private final EmailImportService emailImportService;
    private final ImapPollingProperties props;

    ImapPollingJob(EmailImportService emailImportService, ImapPollingProperties props) {
        this.emailImportService = emailImportService;
        this.props = props;
    }

    @Scheduled(fixedDelayString = "${app.imap.polling.interval-ms:60000}")
    void poll() {
        log.info("IMAP poll started: {}@{}:{}", props.getUsername(), props.getHost(), props.getPort());
        String protocol = props.isSsl() ? "imaps" : "imap";
        Properties mailProps = buildMailProperties(protocol);
        Session session = Session.getInstance(mailProps);
        Store store = null;
        Folder folder = null;
        try {
            store = session.getStore(protocol);
            store.connect(props.getHost(), props.getPort(), props.getUsername(), props.getPassword());
            folder = store.getFolder(props.getFolder());
            folder.open(Folder.READ_WRITE);
            Message[] unseen = folder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
            log.debug("IMAP poll: {} unseen message(s) found", unseen.length);
            processMessages(unseen);
        } catch (MessagingException e) {
            log.error("IMAP poll error connecting to {}:{}", props.getHost(), props.getPort(), e);
        } finally {
            closeFolderQuietly(folder);
            closeStoreQuietly(store);
        }
    }

    // package-private for unit testing
    void processMessages(Message[] messages) {
        int imported = 0;
        for (Message msg : messages) {
            if (!(msg instanceof MimeMessage mimeMessage)) {
                continue;
            }
            try {
                var result = emailImportService.importMessage(mimeMessage);
                if (result.isPresent()) {
                    msg.setFlag(Flags.Flag.SEEN, true);
                    imported++;
                }
            } catch (Exception e) {
                log.warn("Failed to import message; skipping", e);
            }
        }
        log.info("IMAP poll complete: {}/{} messages imported", imported, messages.length);
    }

    private Properties buildMailProperties(String protocol) {
        Properties p = new Properties();
        p.setProperty("mail.store.protocol", protocol);
        p.setProperty("mail." + protocol + ".host", props.getHost());
        p.setProperty("mail." + protocol + ".port", String.valueOf(props.getPort()));
        p.setProperty("mail." + protocol + ".ssl.enable", String.valueOf(props.isSsl()));
        p.setProperty("mail." + protocol + ".connectiontimeout", "10000");
        p.setProperty("mail." + protocol + ".timeout", "10000");
        return p;
    }

    private void closeFolderQuietly(Folder folder) {
        if (folder != null && folder.isOpen()) {
            try { folder.close(false); } catch (MessagingException ignored) {}
        }
    }

    private void closeStoreQuietly(Store store) {
        if (store != null && store.isConnected()) {
            try { store.close(); } catch (MessagingException ignored) {}
        }
    }
}
