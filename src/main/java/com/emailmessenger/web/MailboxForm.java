package com.emailmessenger.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MailboxForm {

    @NotBlank(message = "IMAP host is required.")
    @Size(max = 255, message = "IMAP host must be at most 255 characters.")
    private String host;

    @Min(value = 1, message = "Port must be between 1 and 65535.")
    @Max(value = 65535, message = "Port must be between 1 and 65535.")
    private int port = 993;

    private boolean ssl = true;

    @NotBlank(message = "Username is required.")
    @Size(max = 254, message = "Username must be at most 254 characters.")
    private String username;

    @NotBlank(message = "Password is required.")
    private String password;

    /** Wizard provider slug, carried through validation re-renders. */
    private String provider;

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host == null ? null : host.trim(); }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public boolean isSsl() { return ssl; }
    public void setSsl(boolean ssl) { this.ssl = ssl; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username == null ? null : username.trim(); }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider == null ? null : provider.trim(); }
}
