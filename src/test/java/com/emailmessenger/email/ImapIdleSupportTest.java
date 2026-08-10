package com.emailmessenger.email;

import com.emailmessenger.billing.PlanLimitService;
import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.MailAccountRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImapIdleSupportTest {

    private final MailAccountRepository accounts = mock(MailAccountRepository.class);
    private final PlanLimitService planLimitService = mock(PlanLimitService.class);
    private final ImapCredentialsResolver resolver = mock(ImapCredentialsResolver.class);
    private final ImapIdleSupport support =
            new ImapIdleSupport(accounts, planLimitService, resolver);

    @Test
    void onlyPaidTierAccountsAreEligibleForIdle() {
        MailAccount paid = accountOwnedBy(1L, Plan.PRO);
        MailAccount free = accountOwnedBy(2L, Plan.FREE);
        MailAccount business = accountOwnedBy(3L, Plan.BUSINESS);
        when(accounts.findByPollingSuspendedFalse()).thenReturn(List.of(paid, free, business));

        assertThat(support.eligibleAccountIds()).containsExactly(1L, 3L);
    }

    @Test
    void credentialsForResolvesAndPersistsRotatedSecrets() {
        MailAccount account = accountOwnedBy(5L, Plan.PRO);
        ImapCredentials creds =
                ImapCredentials.password("imap.example.com", 993, true, "u@example.com", "pw");
        when(accounts.findById(5L)).thenReturn(Optional.of(account));
        when(resolver.resolve(account)).thenReturn(creds);

        assertThat(support.credentialsFor(5L)).isEqualTo(creds);
        // Saved so an OAuth refresh-token rotation done by the resolver sticks.
        verify(accounts).save(account);
    }

    @Test
    void credentialsForMissingAccountThrowsConnectionException() {
        when(accounts.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> support.credentialsFor(9L))
                .isInstanceOf(ImapConnectionException.class);
    }

    private MailAccount accountOwnedBy(Long accountId, Plan plan) {
        User user = mock(User.class);
        MailAccount account = mock(MailAccount.class);
        when(account.getId()).thenReturn(accountId);
        when(account.getUser()).thenReturn(user);
        when(planLimitService.currentPlan(user)).thenReturn(plan);
        return account;
    }
}
