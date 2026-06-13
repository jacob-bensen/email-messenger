package com.emailmessenger.admin;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminAuthorizerTest {

    private final AdminProperties properties = new AdminProperties();
    private final AdminAuthorizer authorizer = new AdminAuthorizer(properties);

    @Test
    void emptyAllowlistMatchesNoOne() {
        assertThat(authorizer.isAdmin("op@example.com")).isFalse();
    }

    @Test
    void allowlistedEmailMatches() {
        properties.setEmails(List.of("op@example.com"));

        assertThat(authorizer.isAdmin("op@example.com")).isTrue();
    }

    @Test
    void matchIsCaseAndWhitespaceInsensitive() {
        properties.setEmails(List.of("  OP@Example.COM  "));

        assertThat(authorizer.isAdmin("op@example.com")).isTrue();
        assertThat(authorizer.isAdmin("OP@EXAMPLE.COM")).isTrue();
        assertThat(authorizer.isAdmin("  op@example.com  ")).isTrue();
    }

    @Test
    void unlistedEmailDoesNotMatch() {
        properties.setEmails(List.of("op@example.com"));

        assertThat(authorizer.isAdmin("intruder@example.com")).isFalse();
    }

    @Test
    void blankAndNullInputsReturnFalse() {
        properties.setEmails(List.of("op@example.com"));

        assertThat(authorizer.isAdmin(null)).isFalse();
        assertThat(authorizer.isAdmin("")).isFalse();
        assertThat(authorizer.isAdmin("   ")).isFalse();
    }

    @Test
    void setEmailsTreatsNullListAsEmpty() {
        properties.setEmails(null);

        assertThat(properties.getEmails()).isEmpty();
        assertThat(authorizer.isAdmin("op@example.com")).isFalse();
    }

    @Test
    void requireAdminThrowsNoSuchElementOnNonAdmin() {
        properties.setEmails(List.of("op@example.com"));

        assertThatThrownBy(() -> authorizer.requireAdmin("intruder@example.com"))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void requireAdminReturnsSilentlyForAdmin() {
        properties.setEmails(List.of("op@example.com"));

        authorizer.requireAdmin("op@example.com");
    }
}
