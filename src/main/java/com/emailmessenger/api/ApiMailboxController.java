package com.emailmessenger.api;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mailboxes")
class ApiMailboxController {

    private final ApiMailboxService mailboxes;

    ApiMailboxController(ApiMailboxService mailboxes) {
        this.mailboxes = mailboxes;
    }

    @GetMapping
    ApiDtos.MailboxList list(@AuthenticationPrincipal ApiPrincipal principal) {
        return mailboxes.list(principal);
    }

    @GetMapping("/providers")
    ApiDtos.ProviderList providers() {
        return mailboxes.providers();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiDtos.MailboxResponse connect(@AuthenticationPrincipal ApiPrincipal principal,
                                    @RequestBody ApiDtos.ConnectMailboxRequest request) {
        return mailboxes.connect(principal, request);
    }

    @PostMapping("/{id}/sync")
    ApiDtos.SyncResponse sync(@AuthenticationPrincipal ApiPrincipal principal, @PathVariable long id) {
        return mailboxes.sync(principal, id);
    }

    @PutMapping("/preferences")
    ApiDtos.MailboxList updatePreferences(@AuthenticationPrincipal ApiPrincipal principal,
                                          @RequestBody ApiDtos.MailPreferencesRequest request) {
        return mailboxes.updatePreferences(principal, request);
    }

    @PutMapping("/{id}/signature")
    ApiDtos.MailboxResponse updateSignature(@AuthenticationPrincipal ApiPrincipal principal, @PathVariable long id,
                                            @RequestBody ApiDtos.SignatureRequest request) {
        return mailboxes.updateSignature(principal, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void remove(@AuthenticationPrincipal ApiPrincipal principal, @PathVariable long id) {
        mailboxes.remove(principal, id);
    }
}
