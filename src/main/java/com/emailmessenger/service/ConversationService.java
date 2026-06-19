package com.emailmessenger.service;

import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.Message;
import com.emailmessenger.domain.Participant;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        Map<String, Long> idIndex = messageIdIndex(messages);
        // Identity set: EmailThread keeps default equals/hashCode, so this counts
        // distinct threads even when one recurs in non-consecutive runs.
        Set<EmailThread> distinctThreads = Collections.newSetFromMap(new IdentityHashMap<>());

        List<SenderBubbleRun> runs = new ArrayList<>();
        SenderView header = null;
        EmailThread currentThread = null;
        boolean currentOutbound = false;
        SenderView currentRunSender = null;
        List<BubbleMessage> currentBubbles = new ArrayList<>();

        for (Message msg : messages) {
            EmailThread thread = msg.getThread();
            distinctThreads.add(thread);
            boolean outbound = msg.isOutbound();

            // Header identity is the correspondent (the requested address), taken
            // from their first received message rather than an outbound one.
            if (header == null && !outbound
                    && senderEmail.equalsIgnoreCase(msg.getSender().getEmail())) {
                header = SenderView.from(msg.getSender());
            }

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

        if (header == null && !runs.isEmpty()) {
            header = runs.get(0).sender();
        }
        return new SenderConversation(header, distinctThreads.size(), List.copyOf(runs));
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
