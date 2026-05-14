package com.emailmessenger.auth;

import com.emailmessenger.repository.UserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
class DbUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    DbUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        var normalized = UserService.normalizeEmail(username);
        var user = users.findByEmail(normalized)
                .orElseThrow(() -> new UsernameNotFoundException("No user with email: " + username));
        return User.withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .disabled(!user.isEnabled())
                .authorities(Collections.emptyList())
                .build();
    }
}
