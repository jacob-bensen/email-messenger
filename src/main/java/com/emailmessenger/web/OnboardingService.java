package com.emailmessenger.web;

import com.emailmessenger.domain.User;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.repository.MailAccountRepository;
import com.emailmessenger.repository.SavedSearchRepository;
import org.springframework.stereotype.Service;

@Service
class OnboardingService {

    private final MailAccountRepository mailAccountRepository;
    private final EmailThreadRepository threadRepository;
    private final SavedSearchRepository savedSearchRepository;

    OnboardingService(MailAccountRepository mailAccountRepository,
                      EmailThreadRepository threadRepository,
                      SavedSearchRepository savedSearchRepository) {
        this.mailAccountRepository = mailAccountRepository;
        this.threadRepository = threadRepository;
        this.savedSearchRepository = savedSearchRepository;
    }

    OnboardingChecklist checklistFor(User owner) {
        boolean mailbox = mailAccountRepository.countByUser(owner) > 0;
        long threads = threadRepository.countByOwner(owner);
        boolean savedSearch = savedSearchRepository.countByOwner(owner) > 0;
        return new OnboardingChecklist(mailbox, threads, savedSearch);
    }
}
