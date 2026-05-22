package com.emailmessenger.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MailboxForm {

    @NotBlank(message = "IMAP host is required.")
    @Size(max = 255)
    private String host;

    @Min(value = 1, message = "Port must be between 1 and 65535.")
    @Max(value = 65535, message = "Port must be between 1 and 65535.")
    private int port = 993;

    private boolean ssl = true;

    @NotBlank(message = "Username is required.")
    @Size(max = 254)
    private String username;

    @NotBlank(message = "Password is required.")
    private String password;

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
}
