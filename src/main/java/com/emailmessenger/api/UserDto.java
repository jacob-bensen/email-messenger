package com.emailmessenger.api;

import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.User;

import java.util.List;
import java.util.Locale;

record UserDto(
        Long id,
        String email,
        String displayName,
        boolean emailVerified,
        Plan plan,
        List<MailboxDto> mailboxes
) {
    record MailboxDto(Long id, String address) {}

    static UserDto of(User user, Plan plan, List<MailAccount> accounts) {
        List<MailboxDto> mailboxes = accounts.stream()
                .map(a -> new MailboxDto(a.getId(),
                        a.getUsername() == null ? "" : a.getUsername().trim().toLowerCase(Locale.ROOT)))
                .toList();
        return new UserDto(user.getId(), user.getEmail(), user.getDisplayName(),
                user.isEmailVerified(), plan, mailboxes);
    }
}
