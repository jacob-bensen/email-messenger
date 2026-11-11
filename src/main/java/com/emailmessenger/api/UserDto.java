package com.emailmessenger.api;

import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.User;

import java.util.Locale;

record UserDto(Long id, String email, String displayName, boolean emailVerified, String plan) {

    static UserDto from(User user, Plan plan) {
        return new UserDto(user.getId(), user.getEmail(), user.getDisplayName(),
                user.isEmailVerified(), plan.name().toLowerCase(Locale.ROOT));
    }
}
