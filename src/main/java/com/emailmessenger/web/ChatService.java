package com.emailmessenger.web;

import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.Message;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.OwnerAddressService;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.service.ChatConversation;
import com.emailmessenger.service.ConversationService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Loads a single conversation (all messages sharing a {@code conversationKey})
 * as a texting-style timeline. Runs in a read transaction so the lazy thread,
 * sender, recipient, and attachment associations resolve before the view model
 * is handed to the template.
 */
@Service
public class ChatService {

    private final EmailThreadRepository threads;
    private final ConversationService conversationService;
    private final OwnerAddressService ownerAddressService;
    private final ApplicationEventPublisher events;

    ChatService(EmailThreadRepository threads,
                ConversationService conversationService,
                OwnerAddressService ownerAddressService,
                ApplicationEventPublisher events) {
        this.threads = threads;
        this.conversationService = conversationService;
        this.ownerAddressService = ownerAddressService;
        this.events = events;
    }

    /** Timeline order for paging (docs/API.md §5.6): sent-at, missing last, then id. */
    static final Comparator<Message> TIMELINE_ORDER = Comparator
            .comparing(Message::getSentAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Message::getId);

    /** One page of a conversation; {@code totalMessages} counts the whole conversation in scope. */
    public record ChatPage(ChatConversation conversation, int totalMessages, boolean hasMore) {}

    /** The {@code before} cursor is not a message of the conversation in scope. */
    public static final class UnknownMessageException extends RuntimeException {
        UnknownMessageException() {
            super("No such message in this conversation.");
        }
    }

    @Transactional(readOnly = true)
    public ChatConversation buildFor(User owner, String conversationKey) {
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
     * The newest {@code limit} messages older than message {@code before} (or the
     * newest overall when {@code before} is null), scoped to {@code mailboxAddress}
     * when it is set. Null when there is no such conversation.
     *
     * @throws UnknownMessageException when {@code before} is not in the conversation
     */
    @Transactional(readOnly = true)
    public ChatPage page(User owner, String mailboxAddress, String conversationKey, Long before, int limit) {
        if (conversationKey == null || conversationKey.isBlank()) {
            return null;
        }
        List<Message> loaded = mailboxAddress == null || mailboxAddress.isBlank()
                ? threads.findMessagesByConversationKey(owner, conversationKey)
                : threads.findMessagesByConversationKeyForMailbox(
                        owner, mailboxAddress.trim().toLowerCase(Locale.ROOT), conversationKey);
        if (loaded.isEmpty()) {
            return null;
        }
        List<Message> all = new ArrayList<>(loaded);
        all.sort(TIMELINE_ORDER);
        int end = all.size();
        if (before != null) {
            end = indexOf(all, before);
            if (end < 0) {
                throw new UnknownMessageException();
            }
        }
        int start = Math.max(0, end - limit);
        ChatConversation chat = conversationService.buildChatConversation(
                all, all.subList(start, end), ownerAddressService.addressesFor(owner));
        return new ChatPage(chat, all.size(), start > 0);
    }

    private static int indexOf(List<Message> messages, long id) {
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).getId() == id) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Same as {@link #buildFor(User, String)} but scoped to a single connected
     * account: only the conversation's threads involving {@code mailboxAddress}
     * are included, so an account's chat history stays separate from the same
     * correspondent reached through another account.
     */
    @Transactional(readOnly = true)
    public ChatConversation buildFor(User owner, String mailboxAddress, String conversationKey) {
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
     * No-op when nothing is unread; otherwise publishes a
     * {@link ConversationChangedEvent.Kind#READ} event after commit. Returns how many threads the conversation has
     * in that scope, so 0 means there is no such conversation.
     */
    @Transactional
    public int markRead(User owner, String mailboxAddress, String conversationKey) {
        if (conversationKey == null || conversationKey.isBlank()) {
            return 0;
        }
        List<EmailThread> conversationThreads;
        if (mailboxAddress == null || mailboxAddress.isBlank()) {
            conversationThreads = threads.findThreadsByConversationKey(owner, conversationKey);
        } else {
            conversationThreads = threads.findThreadsByConversationKeyForMailbox(
                    owner, mailboxAddress.trim().toLowerCase(Locale.ROOT), conversationKey);
        }
        boolean changed = false;
        for (EmailThread thread : conversationThreads) {
            if (thread.isUnread()) {
                thread.markRead();
                changed = true;
            }
        }
        // Only a real change is published: the website's pane refresh marks
        // read again on every conversation-read event, and must not echo.
        if (changed) {
            events.publishEvent(ConversationChangedEvent.of(
                    owner.getId(), ConversationChangedEvent.Kind.READ, conversationKey));
        }
        return conversationThreads.size();
    }
}
