package com.emailmessenger.web;

public record OnboardingChecklist(
        boolean mailboxConnected,
        boolean firstThreadImported) {

    public boolean isComplete() {
        return mailboxConnected && firstThreadImported;
    }

    public String nextStepCtaUrl() {
        if (!mailboxConnected) {
            return "/mailboxes/new";
        }
        if (!firstThreadImported) {
            return "/mailboxes";
        }
        return "/threads";
    }

    public String nextStepCtaLabel() {
        if (!mailboxConnected) {
            return "Connect your inbox";
        }
        if (!firstThreadImported) {
            return "Sync now";
        }
        return "Open inbox";
    }
}
