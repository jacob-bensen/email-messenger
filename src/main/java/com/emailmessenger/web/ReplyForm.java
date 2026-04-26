package com.emailmessenger.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

class ReplyForm {

    @NotBlank(message = "Reply body cannot be empty")
    @Size(max = 100_000, message = "Reply body is too long")
    private String body = "";

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
}
