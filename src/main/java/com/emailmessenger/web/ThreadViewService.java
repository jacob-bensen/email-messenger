package com.emailmessenger.web;

import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.User;
import com.emailmessenger.service.Conversation;
import com.emailmessenger.service.ConversationService;
import com.emailmessenger.team.ThreadAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
class ThreadViewService {

    private final ThreadAccessService threadAccess;
    private final ConversationService conversationService;

    ThreadViewService(ThreadAccessService threadAccess,
                      ConversationService conversationService) {
        this.threadAccess = threadAccess;
        this.conversationService = conversationService;
    }

    // Read-write: viewing as the owner also marks the thread read (JPA dirty-checks
    // the unread flip on transaction commit). A teammate following a shared link is
    // read-only — their visit doesn't clear the owner's "unread" filter chip.
    @Transactional
    Conversation getConversation(long threadId, User viewer) {
        EmailThread thread = threadAccess.findAccessibleThread(threadId, viewer)
                .orElseThrow(NoSuchElementException::new);
        if (threadAccess.isOwner(thread, viewer)) {
            thread.markRead();
        }
        return conversationService.buildConversation(thread);
    }
}
