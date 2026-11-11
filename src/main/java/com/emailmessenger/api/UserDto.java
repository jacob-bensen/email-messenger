package com.emailmessenger.api;

import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.User;

record UserDto(Long id, String email, String displayName, boolean emailVerified, Plan plan) {

    static UserDto from(User user, Plan plan) {
        return new UserDto(user.getId(), user.getEmail(), user.getDisplayName(),
                user.isEmailVerified(), plan);
    }
}
