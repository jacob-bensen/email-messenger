package com.emailmessenger.web;

import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.Message;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.OwnerAddressService;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.service.ChatConversation;
import com.emailmessenger.service.ConversationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Loads a single conversation (all messages sharing a {@code conversationKey})
 * as a texting-style timeline. Runs in a read transaction so the lazy thread,
 * sender, recipient, and attachment associations resolve before the view model
 * is handed to the template.
 */
@Service
class ChatService {

    private final EmailThreadRepository threads;
    private final ConversationService conversationService;
    private final OwnerAddressService ownerAddressService;

    ChatService(EmailThreadRepository threads,
                ConversationService conversationService,
                OwnerAddressService ownerAddressService) {
        this.threads = threads;
        this.conversationService = conversationService;
        this.ownerAddressService = ownerAddressService;
    }

    @Transactional(readOnly = true)
    ChatConversation buildFor(User owner, String conversationKey) {
        if (conversationKey == null || conversationKey.isBlank()) {
            return null;
        }
        List<Message> messages = threads.findMessagesByConversationKey(owner, conversationKey);
        if (messages.isEmpty()) {
            return null;
        }
        return conversationService.buildChatConversation(
                messages, ownerAddressService.addressesFor(owner));
    }

    /**
     * Same as {@link #buildFor(User, String)} but scoped to a single connected
     * account: only the conversation's threads involving {@code mailboxAddress}
     * are included, so an account's chat history stays separate from the same
     * correspondent reached through another account.
     */
    @Transactional(readOnly = true)
    ChatConversation buildFor(User owner, String mailboxAddress, String conversationKey) {
        if (conversationKey == null || conversationKey.isBlank()) {
            return null;
        }
        String mbox = mailboxAddress == null ? "" : mailboxAddress.trim().toLowerCase(Locale.ROOT);
        List<Message> messages = threads.findMessagesByConversationKeyForMailbox(owner, mbox, conversationKey);
        if (messages.isEmpty()) {
            return null;
        }
        return conversationService.buildChatConversation(
                messages, ownerAddressService.addressesFor(owner));
    }

    /**
     * Marks the opened conversation's threads read. Scoped to {@code mailboxAddress}
     * when an account is selected (so the same correspondent reached via another
     * account keeps its own unread state); otherwise marks every thread of the key.
     * No-op when nothing is unread.
     */
    @Transactional
    void markRead(User owner, String mailboxAddress, String conversationKey) {
        if (conversationKey == null || conversationKey.isBlank()) {
            return;
        }
        List<EmailThread> conversationThreads;
        if (mailboxAddress == null || mailboxAddress.isBlank()) {
            conversationThreads = threads.findThreadsByConversationKey(owner, conversationKey);
        } else {
            conversationThreads = threads.findThreadsByConversationKeyForMailbox(
                    owner, mailboxAddress.trim().toLowerCase(Locale.ROOT), conversationKey);
        }
        for (EmailThread thread : conversationThreads) {
            if (thread.isUnread()) {
                thread.markRead();
            }
        }
    }
}
