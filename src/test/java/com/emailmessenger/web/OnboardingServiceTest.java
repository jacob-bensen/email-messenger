package com.emailmessenger.web;

import com.emailmessenger.domain.User;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.repository.MailAccountRepository;
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

    OnboardingService service;
    User owner;

    @BeforeEach
    void setUp() {
        service = new OnboardingService(mailAccountRepository, threadRepository);
        owner = new User("owner@example.com", "hash", "Owner");
    }

    @Test
    void freshUserHasNothingComplete() {
        when(mailAccountRepository.countByUser(owner)).thenReturn(0L);
        when(threadRepository.countByOwner(owner)).thenReturn(0L);

        OnboardingChecklist checklist = service.checklistFor(owner);

        assertThat(checklist.mailboxConnected()).isFalse();
        assertThat(checklist.firstThreadImported()).isFalse();
        assertThat(checklist.isComplete()).isFalse();
        assertThat(checklist.nextStepCtaUrl()).isEqualTo("/mailboxes/new");
        assertThat(checklist.nextStepCtaLabel()).isEqualTo("Connect your inbox");
    }

    @Test
    void mailboxConnectedButNoThreadsYetPointsToSync() {
        when(mailAccountRepository.countByUser(owner)).thenReturn(1L);
        when(threadRepository.countByOwner(owner)).thenReturn(0L);

        OnboardingChecklist checklist = service.checklistFor(owner);

        assertThat(checklist.mailboxConnected()).isTrue();
        assertThat(checklist.firstThreadImported()).isFalse();
        assertThat(checklist.isComplete()).isFalse();
        assertThat(checklist.nextStepCtaUrl()).isEqualTo("/mailboxes");
        assertThat(checklist.nextStepCtaLabel()).isEqualTo("Sync now");
    }

    @Test
    void mailboxAndThreadsBothPresentIsComplete() {
        when(mailAccountRepository.countByUser(owner)).thenReturn(2L);
        when(threadRepository.countByOwner(owner)).thenReturn(17L);

        OnboardingChecklist checklist = service.checklistFor(owner);

        assertThat(checklist.mailboxConnected()).isTrue();
        assertThat(checklist.firstThreadImported()).isTrue();
        assertThat(checklist.isComplete()).isTrue();
    }
}
