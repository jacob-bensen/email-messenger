package com.emailmessenger.web;

import java.util.Optional;

/**
 * IMAP-provider presets shown on the connect-a-mailbox wizard. Each entry
 * carries the dial-in details we use to pre-fill the credentials form,
 * plus a deep link to the provider's app-password page since the modern
 * Gmail/iCloud/Outlook accounts all require one.
 */
public enum MailboxProvider {

    GMAIL("gmail", "Gmail",
            "imap.gmail.com", 993, true,
            "https://myaccount.google.com/apppasswords",
            "Gmail accepts a 16-character App Password (turn on 2-Step Verification first)."),

    ICLOUD("icloud", "iCloud Mail",
            "imap.mail.me.com", 993, true,
            "https://account.apple.com/account/manage",
            "iCloud requires an app-specific password from appleid.apple.com → Sign-In and Security."),

    FASTMAIL("fastmail", "Fastmail",
            "imap.fastmail.com", 993, true,
            "https://app.fastmail.com/settings/security/integrations",
            "Fastmail requires an App Password scoped to IMAP. Generate one under Settings → Privacy & Security."),

    OUTLOOK("outlook", "Outlook / Microsoft 365",
            "outlook.office365.com", 993, true,
            "https://account.microsoft.com/security",
            "Outlook.com / Microsoft 365 requires an app password (with 2-step verification enabled)."),

    OTHER("other", "Other IMAP server",
            "", 993, true,
            null,
            "Enter your provider's IMAP host. Most servers use port 993 with TLS.");

    private final String slug;
    private final String displayName;
    private final String host;
    private final int port;
    private final boolean ssl;
    private final String appPasswordUrl;
    private final String appPasswordHelp;

    MailboxProvider(String slug, String displayName, String host, int port, boolean ssl,
                    String appPasswordUrl, String appPasswordHelp) {
        this.slug = slug;
        this.displayName = displayName;
        this.host = host;
        this.port = port;
        this.ssl = ssl;
        this.appPasswordUrl = appPasswordUrl;
        this.appPasswordHelp = appPasswordHelp;
    }

    public String getSlug() { return slug; }
    public String getDisplayName() { return displayName; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public boolean isSsl() { return ssl; }
    public String getAppPasswordUrl() { return appPasswordUrl; }
    public String getAppPasswordHelp() { return appPasswordHelp; }

    public static Optional<MailboxProvider> fromSlug(String slug) {
        if (slug == null) {
            return Optional.empty();
        }
        String normalized = slug.trim().toLowerCase();
        for (MailboxProvider p : values()) {
            if (p.slug.equals(normalized)) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }
}
