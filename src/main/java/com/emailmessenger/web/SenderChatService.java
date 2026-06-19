package com.emailmessenger.web;

import com.emailmessenger.domain.Message;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.service.ConversationService;
import com.emailmessenger.service.SenderConversation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Assembles the cross-thread chat for a single sender. Runs in a read
 * transaction so the lazy {@code thread}, {@code sender}, and {@code attachments}
 * associations resolve before the conversation is handed to the template.
 */
@Service
class SenderChatService {

    private final EmailThreadRepository threads;
    private final ConversationService conversationService;

    SenderChatService(EmailThreadRepository threads, ConversationService conversationService) {
        this.threads = threads;
        this.conversationService = conversationService;
    }

    @Transactional(readOnly = true)
    SenderConversation buildFor(User owner, String senderEmail) {
        if (senderEmail == null || senderEmail.isBlank()) {
            return null;
        }
        List<Message> messages = threads.findConversationWithSender(owner, senderEmail);
        if (messages.isEmpty()) {
            return null;
        }
        return conversationService.buildSenderConversation(messages, senderEmail);
    }
}
