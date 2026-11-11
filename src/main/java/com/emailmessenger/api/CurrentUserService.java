package com.emailmessenger.api;

import com.emailmessenger.billing.PlanLimitService;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.MailAccountService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Assembles the signed-in user's profile for the login response and {@code /me}. */
@Service
class CurrentUserService {

    private final PlanLimitService planLimitService;
    private final MailAccountService mailAccountService;

    CurrentUserService(PlanLimitService planLimitService, MailAccountService mailAccountService) {
        this.planLimitService = planLimitService;
        this.mailAccountService = mailAccountService;
    }

    @Transactional(readOnly = true)
    UserDto describe(User user) {
        return UserDto.of(user, planLimitService.currentPlan(user), mailAccountService.list(user));
    }
}
