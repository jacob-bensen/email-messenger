package com.emailmessenger.auth;

import com.emailmessenger.domain.User;
import com.emailmessenger.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class UserActivityServiceTest {

    @Autowired UserActivityService activity;
    @Autowired UserService userService;
    @Autowired UserRepository users;

    @Test
    void recordInboxVisitStampsLastInboxVisitAt() {
        userService.register("visit@example.com", "password1", "Visit");
        User before = users.findByEmail("visit@example.com").orElseThrow();
        assertThat(before.getLastInboxVisitAt()).isNull();

        activity.recordInboxVisit(before);

        User after = users.findByEmail("visit@example.com").orElseThrow();
        assertThat(after.getLastInboxVisitAt()).isNotNull();
    }

    @Test
    void recordLoginIsCaseInsensitiveAndStampsLastLoginAt() {
        userService.register("login@example.com", "password1", "Login");

        activity.recordLogin("LOGIN@Example.com");

        User after = users.findByEmail("login@example.com").orElseThrow();
        assertThat(after.getLastLoginAt()).isNotNull();
    }

    @Test
    void recordLoginIgnoresNullAndBlankAndAnonymous() {
        userService.register("noop@example.com", "password1", "Noop");

        activity.recordLogin(null);
        activity.recordLogin("   ");

        User after = users.findByEmail("noop@example.com").orElseThrow();
        assertThat(after.getLastLoginAt()).isNull();
    }

    @Test
    void recordInboxVisitOnUnsavedUserIsNoop() {
        activity.recordInboxVisit(null);
        activity.recordInboxVisit(new User("unsaved@example.com", "h", "U"));
        // no exception, no row changed
    }
}
