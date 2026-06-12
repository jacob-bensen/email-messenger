package com.emailmessenger.web;

import com.emailmessenger.domain.User;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.repository.MailAccountRepository;
import com.emailmessenger.repository.SavedSearchRepository;
import com.emailmessenger.repository.TeamInviteRepository;
import org.springframework.stereotype.Service;

@Service
class OnboardingService {

    private final MailAccountRepository mailAccountRepository;
    private final EmailThreadRepository threadRepository;
    private final SavedSearchRepository savedSearchRepository;
    private final TeamInviteRepository teamInviteRepository;

    OnboardingService(MailAccountRepository mailAccountRepository,
                      EmailThreadRepository threadRepository,
                      SavedSearchRepository savedSearchRepository,
                      TeamInviteRepository teamInviteRepository) {
        this.mailAccountRepository = mailAccountRepository;
        this.threadRepository = threadRepository;
        this.savedSearchRepository = savedSearchRepository;
        this.teamInviteRepository = teamInviteRepository;
    }

    OnboardingChecklist checklistFor(User owner) {
        boolean mailbox = mailAccountRepository.countByUser(owner) > 0;
        long threads = threadRepository.countByOwner(owner);
        boolean savedSearch = savedSearchRepository.countByOwner(owner) > 0;
        boolean teammateInvited = teamInviteRepository.countNonRevokedByInviter(owner) > 0;
        return new OnboardingChecklist(mailbox, threads, savedSearch, teammateInvited);
    }
}
