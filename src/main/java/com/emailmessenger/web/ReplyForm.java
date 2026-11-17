package com.emailmessenger.web;

/** The chat reply box. Content rules (empty, length) live in {@link ChatSendService}. */
class ReplyForm {

    private String body = "";

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
}
