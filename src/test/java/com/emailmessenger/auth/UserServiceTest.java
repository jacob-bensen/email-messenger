package com.emailmessenger.auth;

import com.emailmessenger.domain.User;
import com.emailmessenger.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({UserService.class, UserServiceTest.TestConfig.class})
class UserServiceTest {

    @Autowired UserRepository users;
    @Autowired UserService userService;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void registersUserWithHashedPassword() {
        User saved = userService.register("Alice@Example.com", "secret123", "Alice");

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("alice@example.com");
        assertThat(saved.getDisplayName()).isEqualTo("Alice");
        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.getPasswordHash()).isNotEqualTo("secret123");
        assertThat(passwordEncoder.matches("secret123", saved.getPasswordHash())).isTrue();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void rejectsDuplicateEmailCaseInsensitive() {
        userService.register("dup@example.com", "password1", null);

        assertThatThrownBy(() -> userService.register("DUP@example.com", "password2", null))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
        assertThat(users.count()).isEqualTo(1);
    }

    @Test
    void blankDisplayNameStoredAsNull() {
        User saved = userService.register("blank@example.com", "password1", "   ");
        assertThat(saved.getDisplayName()).isNull();
    }

    @Test
    void findByEmailIsCaseInsensitive() {
        userService.register("find@example.com", "password1", null);
        assertThat(userService.findByEmail("FIND@Example.com")).isPresent();
    }

    static class TestConfig {
        @org.springframework.context.annotation.Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }
}
