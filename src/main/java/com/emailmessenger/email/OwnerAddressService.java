package com.emailmessenger.email;

import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.MailAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Enumerates every email address that represents "me" for an owner: their
 * account email plus the login address of each connected mailbox. Used to
 * exclude the owner from a conversation's participant set so a chat is keyed by
 * the <em>other</em> people, not by yourself.
 */
@Service
public class OwnerAddressService {

    private final MailAccountRepository mailAccounts;

    OwnerAddressService(MailAccountRepository mailAccounts) {
        this.mailAccounts = mailAccounts;
    }

    @Transactional(readOnly = true)
    public Set<String> addressesFor(User owner) {
        Set<String> addresses = new HashSet<>();
        if (owner.getEmail() != null && !owner.getEmail().isBlank()) {
            addresses.add(owner.getEmail().trim().toLowerCase(Locale.ROOT));
        }
        for (MailAccount account : mailAccounts.findByUserOrderByCreatedAtAsc(owner)) {
            if (account.getUsername() != null && !account.getUsername().isBlank()) {
                addresses.add(account.getUsername().trim().toLowerCase(Locale.ROOT));
            }
        }
        return addresses;
    }
}
