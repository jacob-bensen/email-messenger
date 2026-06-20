package com.emailmessenger.web;

import com.emailmessenger.domain.Message;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.OwnerAddressService;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.service.ChatConversation;
import com.emailmessenger.service.ConversationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
}
