package com.emailmessenger.web;

import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.domain.Message;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.MailAccountService;
import com.emailmessenger.email.OwnerAddressService;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.service.ChatConversation;
import com.emailmessenger.service.ChatMember;
import com.emailmessenger.service.ConversationKeyService;
import com.emailmessenger.service.OutgoingAttachment;
import com.emailmessenger.service.ReplyService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The send flow shared by the website's chat compose boxes and
 * {@code /api/v1} (docs/API.md §5.8–5.9): validation, recipients, sending
 * account, signature, default subject, reply-vs-new. The email is sent
 * before it is recorded, so a mail-server failure ({@code MailException})
 * leaves nothing behind. Not transactional itself: SMTP must not run inside
 * a database transaction; each record call is its own transaction.
 */
@Service
public class ChatSendService {

    public static final int MAX_BODY_LENGTH = 100_000;
    static final String DEFAULT_SUBJECT = "(no subject)";

    public enum Rejection {
        NO_RECIPIENTS("Add at least one valid recipient email address."),
        EMPTY_MESSAGE("Add a message or an attachment."),
        BODY_TOO_LONG("The message is too long."),
        NO_SUCH_CONVERSATION("No such conversation."),
        NO_MEMBERS("This conversation has no one to send to.");

        private final String message;

        Rejection(String message) {
            this.message = message;
        }

        public String message() {
            return message;
        }
    }

    public static class SendRejectedException extends RuntimeException {

        private final Rejection reason;

        SendRejectedException(Rejection reason) {
            super(reason.message());
            this.reason = reason;
        }

        public Rejection reason() {
            return reason;
        }
    }

    /** {@code conversationKey} is the conversation the recorded message landed in. */
    public record SendResult(String conversationKey, Long threadId, Long messageId) {}

    private final ChatService chatService;
    private final EmailThreadRepository threads;
    private final ReplyService replyService;
    private final OutboundMessageService outbound;
    private final MailAccountService mailAccounts;
    private final OwnerAddressService ownerAddresses;
    private final ConversationKeyService keys;
    private final ApplicationEventPublisher events;

    ChatSendService(ChatService chatService, EmailThreadRepository threads, ReplyService replyService,
                    OutboundMessageService outbound, MailAccountService mailAccounts,
                    OwnerAddressService ownerAddresses, ConversationKeyService keys,
                    ApplicationEventPublisher events) {
        this.chatService = chatService;
        this.threads = threads;
        this.replyService = replyService;
        this.outbound = outbound;
        this.mailAccounts = mailAccounts;
        this.ownerAddresses = ownerAddresses;
        this.keys = keys;
        this.events = events;
    }

    /**
     * Sends into an existing conversation, to all its members. {@code selected}
     * (may be null) scopes the conversation and picks the sending account.
     * {@code asReply} continues the most recently active thread; otherwise a
     * new email with {@code subject} is pinned to this conversation.
     */
    public SendResult sendToConversation(User owner, MailAccount selected, String key, String body,
                                         String subject, boolean asReply,
                                         List<OutgoingAttachment> attachments) {
        List<OutgoingAttachment> files = attachments == null ? List.of() : attachments;
        String text = requireContent(body, files);
        List<MailAccount> accounts = mailAccounts.list(owner);
        String scope = ConversationListService.scopeAddress(selected, accounts);
        ChatConversation conversation = scope != null
                ? chatService.buildFor(owner, scope, key)
                : chatService.buildFor(owner, key);
        if (conversation == null) {
            throw new SendRejectedException(Rejection.NO_SUCH_CONVERSATION);
        }
        List<String> recipients = conversation.members().stream().map(ChatMember::email).toList();
        if (recipients.isEmpty()) {
            throw new SendRejectedException(Rejection.NO_MEMBERS);
        }
        MailAccount sender = sendingAccount(owner, accounts, selected);
        String from = sender != null ? sender.getUsername() : owner.getEmail();
        String outgoingBody = withSignature(text, sender);

        List<EmailThread> conversationThreads = threads.findThreadsByConversationKey(owner, key);
        if (asReply && !conversationThreads.isEmpty()) {
            EmailThread target = conversationThreads.get(0);
            replyService.sendReply(target.getId(), target.getSubject(), outgoingBody, files, recipients, from);
            return sent(owner, outbound.recordReply(target.getId(), owner, outgoingBody, files));
        }
        String subj = subjectOrDefault(subject);
        replyService.sendNewEmail(subj, outgoingBody, files, recipients, from);
        return sent(owner, outbound.recordNewEmail(owner, key, subj, outgoingBody, recipients, files));
    }

    /**
     * Sends a brand-new email and files it under the recipients' conversation
     * key, which may be an existing conversation with the same people.
     */
    public SendResult startConversation(User owner, MailAccount selected, List<String> to,
                                        String subject, String body,
                                        List<OutgoingAttachment> attachments) {
        List<OutgoingAttachment> files = attachments == null ? List.of() : attachments;
        List<String> recipients = parseRecipients(to);
        if (recipients.isEmpty()) {
            throw new SendRejectedException(Rejection.NO_RECIPIENTS);
        }
        String text = requireContent(body, files);
        MailAccount sender = sendingAccount(owner, mailAccounts.list(owner), selected);
        String from = sender != null ? sender.getUsername() : owner.getEmail();
        String subj = subjectOrDefault(subject);
        String key = keys.keyFor(recipients, ownerAddresses.addressesFor(owner));
        String outgoingBody = withSignature(text, sender);

        replyService.sendNewEmail(subj, outgoingBody, files, recipients, from);
        return sent(owner, outbound.recordNewEmail(owner, key, subj, outgoingBody, recipients, files));
    }

    /**
     * Each entry is split on commas, semicolons and whitespace; parts without
     * an {@code @} are dropped and duplicates removed, keeping first-seen order.
     */
    static List<String> parseRecipients(List<String> entries) {
        if (entries == null) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String entry : entries) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            for (String part : entry.split("[,;\\s]+")) {
                String email = part.trim();
                if (!email.isEmpty() && email.contains("@") && !out.contains(email)) {
                    out.add(email);
                }
            }
        }
        return out;
    }

    private static String requireContent(String body, List<OutgoingAttachment> attachments) {
        String text = body == null ? "" : body;
        if (text.length() > MAX_BODY_LENGTH) {
            throw new SendRejectedException(Rejection.BODY_TOO_LONG);
        }
        if (text.isBlank() && attachments.isEmpty()) {
            throw new SendRejectedException(Rejection.EMPTY_MESSAGE);
        }
        return text;
    }

    private static String subjectOrDefault(String subject) {
        return subject == null || subject.isBlank() ? DEFAULT_SUBJECT : subject.trim();
    }

    /**
     * The record call has committed by now (no surrounding transaction), so
     * the event goes out immediately and other clients' refetch sees it.
     */
    private SendResult sent(User owner, Message recorded) {
        EmailThread thread = recorded.getThread();
        events.publishEvent(new ConversationChangedEvent(owner.getId(),
                ConversationChangedEvent.Change.SENT, Collections.singletonList(thread.getConversationKey())));
        return new SendResult(thread.getConversationKey(), thread.getId(), recorded.getId());
    }

    /**
     * A mailbox-scoped view wins; otherwise the user's default sending account
     * (if it still exists), then the sole connected mailbox. {@code null} means
     * the From falls back to the login email.
     */
    private static MailAccount sendingAccount(User owner, List<MailAccount> accounts, MailAccount selected) {
        if (selected != null) {
            return selected;
        }
        MailAccount preferred = owner.getDefaultMailAccount();
        if (preferred != null) {
            for (MailAccount account : accounts) {
                if (account.getId().equals(preferred.getId())) {
                    return account;
                }
            }
        }
        return accounts.size() == 1 ? accounts.get(0) : null;
    }

    /**
     * Applied before both the SMTP send and the recorded copy, so the bubble
     * matches what the recipient received.
     */
    private static String withSignature(String body, MailAccount sender) {
        if (sender == null || sender.getSignature() == null || sender.getSignature().isBlank()) {
            return body;
        }
        return body + "\n\n-- \n" + sender.getSignature().strip();
    }
}
