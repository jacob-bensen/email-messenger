package com.emailmessenger.email;

import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.MailAccountRepository;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

/**
 * "Sync now" for one mailbox, shared by {@code POST /mailboxes/{id}/sync} and
 * {@code POST /api/v1/mailboxes/{id}/sync}. Not transactional: the poll runs
 * in its own transaction, and the reload must see what it committed.
 */
@Service
public class MailboxSyncService {

    private final MailAccountRepository accounts;
    private final MailboxPollingService polling;

    MailboxSyncService(MailAccountRepository accounts, MailboxPollingService polling) {
        this.accounts = accounts;
        this.polling = polling;
    }

    /**
     * Polls the user's mailbox now. A failed poll is not an exception: it is
     * recorded on the account ({@code lastSyncError}) like a scheduled poll.
     *
     * @throws NoSuchElementException if {@code accountId} is not one of the user's mailboxes
     */
    public SyncOutcome syncNow(User owner, Long accountId) {
        MailAccount account = accounts.findByIdAndUser(accountId, owner)
                .orElseThrow(NoSuchElementException::new);
        int imported = polling.pollOne(account.getId());
        MailAccount reloaded = accounts.findById(account.getId())
                .orElseThrow(NoSuchElementException::new);
        return new SyncOutcome(reloaded, imported);
    }

    /** {@code imported} counts received and sent messages new to the inbox. */
    public record SyncOutcome(MailAccount account, int imported) {}
}
