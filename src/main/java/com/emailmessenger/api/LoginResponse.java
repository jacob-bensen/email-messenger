package com.emailmessenger.api;

import java.time.Instant;

record LoginResponse(String token, Instant expiresAt, UserDto user) {}
