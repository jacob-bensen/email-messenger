package com.emailmessenger.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

class WaitlistForm {

    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address")
    @Size(max = 254, message = "Email must be 254 characters or fewer")
    private String email = "";

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email == null ? "" : email.strip(); }
}
