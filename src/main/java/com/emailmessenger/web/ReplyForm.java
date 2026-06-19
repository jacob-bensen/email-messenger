package com.emailmessenger.web;

import jakarta.validation.constraints.Size;

class ReplyForm {

    // Not @NotBlank: an attachment-only reply (empty body) is allowed. The
    // controller rejects only the case where both the body and attachments
    // are empty.
    @Size(max = 100_000, message = "Reply body is too long")
    private String body = "";

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
}
