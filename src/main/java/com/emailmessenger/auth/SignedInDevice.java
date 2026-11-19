package com.emailmessenger.auth;

import java.time.LocalDateTime;

/** One API token, as the user sees it: an app they are signed in to. */
public record SignedInDevice(long id, String deviceName, LocalDateTime signedInAt,
                             LocalDateTime lastUsedAt, LocalDateTime expiresAt, boolean current) {}
