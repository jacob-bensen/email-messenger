package com.emailmessenger.service;

import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.Message;
import com.emailmessenger.domain.MessageRecipient;
import com.emailmessenger.domain.Participant;
import com.emailmessenger.domain.RecipientType;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ConversationService {

    private final IMTransformService imTransform;
    private final EmailBodyCleaner bodyCleaner = new EmailBodyCleaner();

    ConversationService(IMTransformService imTransform) {
        this.imTransform = imTransform;
    }

    public Conversation buildConversation(EmailThread thread) {
        List<Message> messages = thread.getMessages(); // @OrderBy sentAt ASC
        Map<String, Long> idIndex = messageIdIndex(messages);

        List<BubbleRun> runs = new ArrayList<>();
        Participant currentSender = null;
        boolean currentOutbound = false;
        List<BubbleMessage> currentBubbles = new ArrayList<>();

        for (Message msg : messages) {
            BubbleMessage bubble = toBubble(msg, idIndex);

            Participant sender = msg.getSender();
            // New run when the sender changes OR the direction flips, so an
            // outbound reply never merges into an inbound run from the same address.
            if (currentSender == null
                    || !currentSender.getEmail().equals(sender.getEmail())
                    || currentOutbound != msg.isOutbound()) {
                if (currentSender != null) {
                    runs.add(new BubbleRun(SenderView.from(currentSender),
                            List.copyOf(currentBubbles), currentOutbound));
                }
                currentSender = sender;
                currentOutbound = msg.isOutbound();
                currentBubbles = new ArrayList<>();
            }
            currentBubbles.add(bubble);
        }

        if (currentSender != null) {
            runs.add(new BubbleRun(SenderView.from(currentSender),
                    List.copyOf(currentBubbles), currentOutbound));
        }

        return new Conversation(thread, List.copyOf(runs));
    }

    /**
     * Builds the full back-and-forth with one address as a single chronological
     * timeline: the correspondent's received messages interleaved with the
     * owner's own outbound replies. A new run starts when the thread changes or
     * the direction flips, so each run is one side of one thread and can be
     * badged with its thread and styled as received vs. "you". {@code senderEmail}
     * is the correspondent, used for the header identity.
     */
    public SenderConversation buildSenderConversation(List<Message> messages, String senderEmail) {
        BuiltRuns built = buildRuns(messages);

        // Header identity is the correspondent (the requested address), taken
        // from their first received message rather than an outbound one.
        SenderView header = null;
        for (Message msg : messages) {
            if (!msg.isOutbound() && senderEmail.equalsIgnoreCase(msg.getSender().getEmail())) {
                header = SenderView.from(msg.getSender());
                break;
            }
        }
        if (header == null && !built.runs().isEmpty()) {
            header = built.runs().get(0).sender();
        }
        return new SenderConversation(header, built.threadCount(), built.runs(),
                latestSignatureFor(messages, senderEmail));
    }

    /**
     * Builds a texting-style conversation from every message exchanged with one
     * person or group (the same participant set), already merged across threads.
     * {@code ownerAddresses} are the addresses representing "me", excluded from
     * the member list so the chat is keyed by the other people.
     */
    public ChatConversation buildChatConversation(List<Message> messages, Set<String> ownerAddresses) {
        BuiltRuns built = buildRuns(messages);
        List<ChatMember> members = buildMembers(messages, ownerAddresses);
        String key = messages.isEmpty() ? null : messages.get(0).getThread().getConversationKey();
        String initials = members.isEmpty() ? "?" : members.get(0).initials();
        return new ChatConversation(key, titleFor(members), initials,
                members, built.threadCount(), built.runs());
    }

    /** The non-owner participants of the conversation, each with their latest signature. */
    private List<ChatMember> buildMembers(List<Message> messages, Set<String> ownerAddresses) {
        Set<String> owner = normalize(ownerAddresses);
        // First-seen order; upgrade to a participant carrying a display name.
        LinkedHashMap<String, Participant> byEmail = new LinkedHashMap<>();
        for (Message msg : messages) {
            consider(byEmail, msg.getSender(), owner);
            for (MessageRecipient recipient : msg.getRecipients()) {
                if (recipient.getRecipientType() != RecipientType.BCC) {
                    consider(byEmail, recipient.getParticipant(), owner);
                }
            }
        }
        List<ChatMember> members = new ArrayList<>();
        for (Participant p : byEmail.values()) {
            members.add(new ChatMember(p.getEmail(), p.getDisplayName(), p.initials(),
                    labelFor(p), latestSignatureFor(messages, p.getEmail())));
        }
        return members;
    }

    private static void consider(Map<String, Participant> byEmail, Participant p, Set<String> owner) {
        if (p == null || p.getEmail() == null) {
            return;
        }
        String key = p.getEmail().trim().toLowerCase(Locale.ROOT);
        if (key.isEmpty() || owner.contains(key)) {
            return;
        }
        Participant existing = byEmail.get(key);
        boolean existingUnnamed = existing == null
                || existing.getDisplayName() == null || existing.getDisplayName().isBlank();
        boolean candidateNamed = p.getDisplayName() != null && !p.getDisplayName().isBlank();
        if (existing == null || (existingUnnamed && candidateNamed)) {
            byEmail.put(key, p);
        }
    }

    private static String titleFor(List<ChatMember> members) {
        return ConversationLabels.title(members.stream().map(ChatMember::label).collect(Collectors.toList()));
    }

    private static String labelFor(Participant p) {
        return (p.getDisplayName() != null && !p.getDisplayName().isBlank())
                ? p.getDisplayName().trim() : p.getEmail();
    }

    private static Set<String> normalize(Set<String> addresses) {
        Set<String> out = new HashSet<>();
        if (addresses != null) {
            for (String a : addresses) {
                if (a != null && !a.isBlank()) {
                    out.add(a.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        return out;
    }

    /**
     * The most recent signature from {@code email}, walking newest-first so the
     * panel reflects their current sign-off. Skips outbound mail and any other
     * address. "" when none was detected.
     */
    private String latestSignatureFor(List<Message> messages, String email) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message msg = messages.get(i);
            if (msg.isOutbound() || !email.equalsIgnoreCase(msg.getSender().getEmail())) {
                continue;
            }
            String signature = signatureOf(msg);
            if (!signature.isBlank()) {
                return signature;
            }
        }
        return "";
    }

    /** Shared bubble-run grouping: a new run per thread change or direction flip. */
    private BuiltRuns buildRuns(List<Message> messages) {
        Map<String, Long> idIndex = messageIdIndex(messages);
        // Identity set: EmailThread keeps default equals/hashCode, so this counts
        // distinct threads even when one recurs in non-consecutive runs.
        Set<EmailThread> distinctThreads = Collections.newSetFromMap(new IdentityHashMap<>());

        List<SenderBubbleRun> runs = new ArrayList<>();
        EmailThread currentThread = null;
        boolean currentOutbound = false;
        SenderView currentRunSender = null;
        List<BubbleMessage> currentBubbles = new ArrayList<>();

        for (Message msg : messages) {
            EmailThread thread = msg.getThread();
            distinctThreads.add(thread);
            boolean outbound = msg.isOutbound();

            if (currentThread == null || currentThread != thread || currentOutbound != outbound) {
                if (currentThread != null) {
                    runs.add(senderRun(currentRunSender, currentThread, currentBubbles, currentOutbound));
                }
                currentThread = thread;
                currentOutbound = outbound;
                currentRunSender = SenderView.from(msg.getSender());
                currentBubbles = new ArrayList<>();
            }
            currentBubbles.add(toBubble(msg, idIndex));
        }
        if (currentThread != null) {
            runs.add(senderRun(currentRunSender, currentThread, currentBubbles, currentOutbound));
        }
        return new BuiltRuns(List.copyOf(runs), distinctThreads.size());
    }

    private record BuiltRuns(List<SenderBubbleRun> runs, int threadCount) {}

    private String signatureOf(Message msg) {
        if (msg.getBodyHtml() != null && !msg.getBodyHtml().isBlank()) {
            return bodyCleaner.extractSignature(msg.getBodyHtml());
        }
        String signature = imTransform.extractSignature(msg.getBodyPlain());
        return signature.isBlank() ? "" : imTransform.renderMarkdown(signature);
    }

    private BubbleMessage toBubble(Message msg, Map<String, Long> idIndex) {
        // List.copyOf forces the lazy attachments collection to load now, while
        // the session is open, so nothing escapes as a proxy.
        return new BubbleMessage(
            msg.getId(), buildBodyHtml(msg), msg.getSentAt(),
            List.copyOf(msg.getAttachments()), msg.isOutbound(),
            replyTargetId(msg, idIndex));
    }

    private SenderBubbleRun senderRun(SenderView sender, EmailThread thread,
                                      List<BubbleMessage> bubbles, boolean outbound) {
        return new SenderBubbleRun(
            sender, new ThreadRef(thread.getId(), thread.getSubject()),
            List.copyOf(bubbles), outbound);
    }

    /** Message-ID header → DB id, for every message in this view. */
    private Map<String, Long> messageIdIndex(List<Message> messages) {
        Map<String, Long> index = new HashMap<>();
        for (Message m : messages) {
            if (m.getMessageIdHeader() != null) {
                index.put(m.getMessageIdHeader(), m.getId());
            }
        }
        return index;
    }

    /** DB id of the in-reply-to target, if it's also present in this view. */
    private Long replyTargetId(Message msg, Map<String, Long> idIndex) {
        if (msg.getInReplyTo() == null) {
            return null;
        }
        Long target = idIndex.get(msg.getInReplyTo());
        return (target != null && !target.equals(msg.getId())) ? target : null;
    }

    private String buildBodyHtml(Message msg) {
        if (msg.getBodyHtml() != null && !msg.getBodyHtml().isBlank()) {
            // Strips quoted reply history + signatures + empty space, then
            // sanitizes — so the bubble shows only what this message added.
            return bodyCleaner.clean(msg.getBodyHtml());
        }
        String stripped = imTransform.stripQuotes(msg.getBodyPlain());
        return imTransform.renderMarkdown(stripped);
    }
}
