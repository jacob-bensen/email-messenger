package com.emailmessenger.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.imap")
public class ImapPollingProperties {

    private String host = "";
    private int port = 993;
    private String username = "";
    private String password = "";
    private boolean ssl = true;
    private String folder = "INBOX";
    private Polling polling = new Polling();

    String getHost() { return host; }
    void setHost(String host) { this.host = host; }

    int getPort() { return port; }
    void setPort(int port) { this.port = port; }

    String getUsername() { return username; }
    void setUsername(String username) { this.username = username; }

    String getPassword() { return password; }
    void setPassword(String password) { this.password = password; }

    boolean isSsl() { return ssl; }
    void setSsl(boolean ssl) { this.ssl = ssl; }

    String getFolder() { return folder; }
    void setFolder(String folder) { this.folder = folder; }

    Polling getPolling() { return polling; }
    void setPolling(Polling polling) { this.polling = polling; }

    static class Polling {
        private boolean enabled = false;
        private long intervalMs = 60000;

        boolean isEnabled() { return enabled; }
        void setEnabled(boolean enabled) { this.enabled = enabled; }

        long getIntervalMs() { return intervalMs; }
        void setIntervalMs(long intervalMs) { this.intervalMs = intervalMs; }
    }
}
