package com.emailmessenger.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record LoginRequest(
        @NotBlank @Size(max = 254) String email,
        @NotBlank @Size(max = 200) String password,
        @Size(max = 100) String deviceName) {}
