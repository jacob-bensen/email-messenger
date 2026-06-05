package com.emailmessenger.web;

import com.emailmessenger.domain.User;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.repository.MailAccountRepository;
import org.springframework.stereotype.Service;

@Service
class OnboardingService {

    private final MailAccountRepository mailAccountRepository;
    private final EmailThreadRepository threadRepository;

    OnboardingService(MailAccountRepository mailAccountRepository,
                      EmailThreadRepository threadRepository) {
        this.mailAccountRepository = mailAccountRepository;
        this.threadRepository = threadRepository;
    }

    OnboardingChecklist checklistFor(User owner) {
        boolean mailbox = mailAccountRepository.countByUser(owner) > 0;
        boolean thread = threadRepository.countByOwner(owner) > 0;
        return new OnboardingChecklist(mailbox, thread);
    }
}
