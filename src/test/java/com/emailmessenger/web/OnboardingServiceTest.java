package com.emailmessenger.web;

import com.emailmessenger.domain.User;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.repository.MailAccountRepository;
import com.emailmessenger.repository.SavedSearchRepository;
import com.emailmessenger.repository.TeamInviteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    @Mock MailAccountRepository mailAccountRepository;
    @Mock EmailThreadRepository threadRepository;
    @Mock SavedSearchRepository savedSearchRepository;
    @Mock TeamInviteRepository teamInviteRepository;

    OnboardingService service;
    User owner;

    @BeforeEach
    void setUp() {
        service = new OnboardingService(mailAccountRepository, threadRepository,
                savedSearchRepository, teamInviteRepository);
        owner = new User("owner@example.com", "hash", "Owner");
    }

    @Test
    void freshUserHasNothingComplete() {
        when(mailAccountRepository.countByUser(owner)).thenReturn(0L);
        when(threadRepository.countByOwner(owner)).thenReturn(0L);
        when(savedSearchRepository.countByOwner(owner)).thenReturn(0L);
        when(teamInviteRepository.countNonRevokedByInviter(owner)).thenReturn(0L);

        OnboardingChecklist checklist = service.checklistFor(owner);

        assertThat(checklist.mailboxConnected()).isFalse();
        assertThat(checklist.threadCount()).isZero();
        assertThat(checklist.threadsImported()).isFalse();
        assertThat(checklist.savedSearchSaved()).isFalse();
        assertThat(checklist.teammateInvited()).isFalse();
        assertThat(checklist.isComplete()).isFalse();
        assertThat(checklist.completedSteps()).isZero();
        assertThat(checklist.totalSteps()).isEqualTo(4);
        assertThat(checklist.percentComplete()).isZero();
        assertThat(checklist.nextStepCtaUrl()).isEqualTo("/mailboxes/new");
        assertThat(checklist.nextStepCtaLabel()).isEqualTo("Connect your inbox");
    }

    @Test
    void mailboxConnectedButNoThreadsYetPointsToSync() {
        when(mailAccountRepository.countByUser(owner)).thenReturn(1L);
        when(threadRepository.countByOwner(owner)).thenReturn(0L);
        when(savedSearchRepository.countByOwner(owner)).thenReturn(0L);
        when(teamInviteRepository.countNonRevokedByInviter(owner)).thenReturn(0L);

        OnboardingChecklist checklist = service.checklistFor(owner);

        assertThat(checklist.mailboxConnected()).isTrue();
        assertThat(checklist.threadsImported()).isFalse();
        assertThat(checklist.completedSteps()).isEqualTo(1);
        assertThat(checklist.nextStepCtaUrl()).isEqualTo("/mailboxes");
        assertThat(checklist.nextStepCtaLabel()).isEqualTo("Sync now");
    }

    @Test
    void belowThreadsTargetStaysInSyncStep() {
        when(mailAccountRepository.countByUser(owner)).thenReturn(1L);
        when(threadRepository.countByOwner(owner)).thenReturn(7L);
        when(savedSearchRepository.countByOwner(owner)).thenReturn(0L);
        when(teamInviteRepository.countNonRevokedByInviter(owner)).thenReturn(0L);

        OnboardingChecklist checklist = service.checklistFor(owner);

        assertThat(checklist.threadsImported()).isFalse();
        assertThat(checklist.threadsRemaining()).isEqualTo(3L);
        assertThat(checklist.nextStepCtaLabel()).isEqualTo("Sync now");
    }

    @Test
    void atThreadsTargetButNoSavedSearchPointsToSaveSearch() {
        when(mailAccountRepository.countByUser(owner)).thenReturn(1L);
        when(threadRepository.countByOwner(owner)).thenReturn(10L);
        when(savedSearchRepository.countByOwner(owner)).thenReturn(0L);
        when(teamInviteRepository.countNonRevokedByInviter(owner)).thenReturn(0L);

        OnboardingChecklist checklist = service.checklistFor(owner);

        assertThat(checklist.threadsImported()).isTrue();
        assertThat(checklist.savedSearchSaved()).isFalse();
        assertThat(checklist.completedSteps()).isEqualTo(2);
        assertThat(checklist.percentComplete()).isEqualTo(50);
        assertThat(checklist.nextStepCtaLabel()).isEqualTo("Save your first search");
        assertThat(checklist.isComplete()).isFalse();
    }

    @Test
    void savedSearchDoneButNoInvitePointsToInviteStep() {
        when(mailAccountRepository.countByUser(owner)).thenReturn(1L);
        when(threadRepository.countByOwner(owner)).thenReturn(10L);
        when(savedSearchRepository.countByOwner(owner)).thenReturn(1L);
        when(teamInviteRepository.countNonRevokedByInviter(owner)).thenReturn(0L);

        OnboardingChecklist checklist = service.checklistFor(owner);

        assertThat(checklist.savedSearchSaved()).isTrue();
        assertThat(checklist.teammateInvited()).isFalse();
        assertThat(checklist.completedSteps()).isEqualTo(3);
        assertThat(checklist.percentComplete()).isEqualTo(75);
        assertThat(checklist.isComplete()).isFalse();
        assertThat(checklist.nextStepCtaUrl()).isEqualTo("/team/invite");
        assertThat(checklist.nextStepCtaLabel()).isEqualTo("Invite a teammate");
    }

    @Test
    void allFourStepsCompleteFlipsIsComplete() {
        when(mailAccountRepository.countByUser(owner)).thenReturn(2L);
        when(threadRepository.countByOwner(owner)).thenReturn(42L);
        when(savedSearchRepository.countByOwner(owner)).thenReturn(1L);
        when(teamInviteRepository.countNonRevokedByInviter(owner)).thenReturn(1L);

        OnboardingChecklist checklist = service.checklistFor(owner);

        assertThat(checklist.mailboxConnected()).isTrue();
        assertThat(checklist.threadsImported()).isTrue();
        assertThat(checklist.savedSearchSaved()).isTrue();
        assertThat(checklist.teammateInvited()).isTrue();
        assertThat(checklist.completedSteps()).isEqualTo(4);
        assertThat(checklist.percentComplete()).isEqualTo(100);
        assertThat(checklist.isComplete()).isTrue();
    }
}
