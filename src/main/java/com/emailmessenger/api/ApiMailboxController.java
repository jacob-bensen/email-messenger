package com.emailmessenger.api;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
        return ApiDtos.ProviderList.all();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiDtos.MailboxResponse connect(@AuthenticationPrincipal ApiPrincipal principal,
                                    @RequestBody ApiDtos.ConnectMailboxRequest request) {
        return mailboxes.connect(principal, request);
    }

    @PostMapping("/{id}/sync")
    ApiDtos.SyncResponse sync(@AuthenticationPrincipal ApiPrincipal principal, @PathVariable String id) {
        return mailboxes.sync(principal, id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal ApiPrincipal principal, @PathVariable String id) {
        mailboxes.delete(principal, id);
    }
}
