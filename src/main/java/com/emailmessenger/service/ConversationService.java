package com.emailmessenger.service;

import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.Message;
import com.emailmessenger.domain.Participant;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class ConversationService {

    private final IMTransformService imTransform;

    ConversationService(IMTransformService imTransform) {
        this.imTransform = imTransform;
    }

    public Conversation buildConversation(EmailThread thread) {
        List<Message> messages = thread.getMessages(); // @OrderBy sentAt ASC

        List<BubbleRun> runs = new ArrayList<>();
        Participant currentSender = null;
        List<BubbleMessage> currentBubbles = new ArrayList<>();

        for (Message msg : messages) {
            String bodyHtml = buildBodyHtml(msg);
            BubbleMessage bubble = new BubbleMessage(
                msg.getId(), bodyHtml, msg.getSentAt(), msg.getAttachments()
            );

            Participant sender = msg.getSender();
            if (currentSender == null || !currentSender.getEmail().equals(sender.getEmail())) {
                if (currentSender != null) {
                    runs.add(new BubbleRun(currentSender, List.copyOf(currentBubbles)));
                }
                currentSender = sender;
                currentBubbles = new ArrayList<>();
            }
            currentBubbles.add(bubble);
        }

        if (currentSender != null) {
            runs.add(new BubbleRun(currentSender, List.copyOf(currentBubbles)));
        }

        return new Conversation(thread, List.copyOf(runs));
    }

    private String buildBodyHtml(Message msg) {
        if (msg.getBodyHtml() != null && !msg.getBodyHtml().isBlank()) {
            // Sanitize with a permissive but safe allowlist — strips <script>, event handlers,
            // iframes, and javascript: URLs that malicious senders embed in HTML email bodies.
            return Jsoup.clean(msg.getBodyHtml(), Safelist.relaxed());
        }
        String stripped = imTransform.stripQuotes(msg.getBodyPlain());
        return imTransform.renderMarkdown(stripped);
    }
}
