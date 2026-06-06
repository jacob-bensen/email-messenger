package com.emailmessenger.web;

import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.User;
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

    // Read-write: viewing a thread also marks it read (JPA dirty-checks the
    // unread flag flip on transaction commit). Powers the "Unread" filter chip.
    @Transactional
    Conversation getConversation(long threadId, User owner) {
        EmailThread thread = threadRepository.findByIdAndOwner(threadId, owner)
                .orElseThrow(NoSuchElementException::new);
        thread.markRead();
        return conversationService.buildConversation(thread);
    }
}
