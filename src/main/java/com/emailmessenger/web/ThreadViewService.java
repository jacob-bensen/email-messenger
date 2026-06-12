package com.emailmessenger.web;

import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.service.Conversation;
import com.emailmessenger.service.ConversationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
class ThreadViewService {

    private final EmailThreadRepository threadRepository;
    private final ConversationService conversationService;

    ThreadViewService(EmailThreadRepository threadRepository,
                      ConversationService conversationService) {
        this.threadRepository = threadRepository;
        this.conversationService = conversationService;
    }

    @Transactional(readOnly = true)
    Conversation getConversation(long threadId) {
        EmailThread thread = threadRepository.findByIdWithMessages(threadId)
                .orElseThrow(NoSuchElementException::new);
        return conversationService.buildConversation(thread);
    }
}
