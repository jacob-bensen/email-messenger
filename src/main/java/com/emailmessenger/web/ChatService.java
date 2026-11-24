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
public class ChatService {

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
    public ChatConversation buildFor(User owner, String conversationKey) {
        return buildFor(owner, null, conversationKey);
    }

    /**
     * Same as {@link #buildFor(User, String)} but scoped to a single connected
     * account when {@code mailboxAddress} is set: only the conversation's threads
     * involving it are included, so an account's chat history stays separate from
     * the same correspondent reached through another account.
     */
    @Transactional(readOnly = true)
    public ChatConversation buildFor(User owner, String mailboxAddress, String conversationKey) {
        List<Message> messages = messages(owner, mailboxAddress, conversationKey);
        if (messages.isEmpty()) {
            return null;
        }
        return conversationService.buildChatConversation(
                messages, ownerAddressService.addressesFor(owner));
    }

    /**
     * One page of the timeline: the {@code limit} messages just before message
     * {@code beforeMessageId} (or the newest {@code limit} when it is null), in
     * the same order and grouping as {@link #buildFor}. {@code null} when there is
     * no such conversation in scope.
     *
     * @throws UnknownCursorException when {@code beforeMessageId} is not a
     *     message of this conversation in scope
     */
    @Transactional(readOnly = true)
    public ChatPage page(User owner, String mailboxAddress, String conversationKey,
                         Long beforeMessageId, int limit) {
        List<Message> messages = messages(owner, mailboxAddress, conversationKey);
        if (messages.isEmpty()) {
            return null;
        }
        int end = messages.size();
        if (beforeMessageId != null) {
            end = indexOf(messages, beforeMessageId);
            if (end < 0) {
                throw new UnknownCursorException();
            }
        }
        int start = Math.max(0, end - limit);
        boolean continuesEarlier = start > 0 && start < end
                && sameRun(messages.get(start - 1), messages.get(start));
        ChatConversation chat = conversationService.buildChatWindow(
                messages, ownerAddressService.addressesFor(owner), start, end);
        return new ChatPage(chat, messages.size(), start > 0, continuesEarlier);
    }

    /**
     * {@code chat.runs()} hold only this page; {@code messageCount} counts the
     * whole conversation. {@code hasMore}: older messages exist before the page.
     * {@code continuesEarlier}: the page's first run carries on the last run of
     * the older page (same thread, same side), so a client joining pages merges
     * the two instead of starting a new run.
     */
    public record ChatPage(ChatConversation chat, int messageCount, boolean hasMore, boolean continuesEarlier) {}

    /** A page cursor that names no message of the conversation in scope. */
    public static final class UnknownCursorException extends RuntimeException {
        UnknownCursorException() {
            super("Not a message of this conversation.");
        }
    }

    private List<Message> messages(User owner, String mailboxAddress, String conversationKey) {
        if (conversationKey == null || conversationKey.isBlank()) {
            return List.of();
        }
        if (mailboxAddress == null) {
            return threads.findMessagesByConversationKey(owner, conversationKey);
        }
        String mbox = mailboxAddress.trim().toLowerCase(Locale.ROOT);
        return threads.findMessagesByConversationKeyForMailbox(owner, mbox, conversationKey);
    }

    private static int indexOf(List<Message> messages, long messageId) {
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).getId() == messageId) {
                return i;
            }
        }
        return -1;
    }

    /** The grouping rule of {@code ConversationService} runs: same thread, same side. */
    private static boolean sameRun(Message a, Message b) {
        return a.getThread().getId().equals(b.getThread().getId()) && a.isOutbound() == b.isOutbound();
    }

    /**
     * Marks the opened conversation's threads read. Scoped to {@code mailboxAddress}
     * when an account is selected (so the same correspondent reached via another
     * account keeps its own unread state); otherwise marks every thread of the key.
     * No-op when nothing is unread. Returns how many threads the conversation has
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
        for (EmailThread thread : conversationThreads) {
            if (thread.isUnread()) {
                thread.markRead();
            }
        }
        return conversationThreads.size();
    }
}
