package com.emailmessenger.domain;

/**
 * How a {@link MailAccount} authenticates to its IMAP server.
 *
 * <p>{@code PASSWORD} stores a user-supplied secret (typically a provider
 * app password) and logs in with plain {@code LOGIN}. {@code XOAUTH2}
 * stores an OAuth <em>refresh</em> token and mints a short-lived access
 * token per sync, authenticating with the {@code XOAUTH2} SASL mechanism —
 * the modern path for Gmail, which no longer accepts a real password.
 */
public enum AuthType {
    PASSWORD,
    XOAUTH2
}
