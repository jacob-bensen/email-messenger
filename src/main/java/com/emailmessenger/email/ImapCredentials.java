package com.emailmessenger.email;

/**
 * Everything {@link ImapClient} needs to open one IMAP session. The
 * {@code secret} is interpreted per {@link Mechanism}: a password (or app
 * password) for {@link Mechanism#PASSWORD}, or a short-lived OAuth access
 * token for {@link Mechanism#XOAUTH2}.
 */
public record ImapCredentials(String host, int port, boolean ssl,
                              String username, Mechanism mechanism, String secret) {

    public enum Mechanism { PASSWORD, XOAUTH2 }

    public static ImapCredentials password(String host, int port, boolean ssl,
                                           String username, String password) {
        return new ImapCredentials(host, port, ssl, username, Mechanism.PASSWORD, password);
    }

    public static ImapCredentials xoauth2(String host, int port, boolean ssl,
                                          String username, String accessToken) {
        return new ImapCredentials(host, port, ssl, username, Mechanism.XOAUTH2, accessToken);
    }
}
