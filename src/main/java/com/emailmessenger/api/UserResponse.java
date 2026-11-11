package com.emailmessenger.api;

import com.emailmessenger.domain.User;

record UserResponse(long id, String email, String displayName, boolean emailVerified) {

    static UserResponse of(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(),
                user.isEmailVerified());
    }
}
