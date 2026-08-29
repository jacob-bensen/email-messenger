package com.emailmessenger.push;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

/**
 * JSON API the /account/notifications page uses to register / drop the
 * browser's PushSubscription. The public-key endpoint is idempotent + safe
 * to poll; it returns 404 (not 500) when VAPID is not configured so the
 * client cleanly falls back to "coming soon" UI on dev boxes.
 */
@RestController
@RequestMapping("/account/notifications/push")
class PushSubscriptionController {

    private final PushSubscriptionService pushSubscriptions;
    private final UserService userService;
    private final VapidProperties vapid;

    PushSubscriptionController(PushSubscriptionService pushSubscriptions,
                               UserService userService,
                               VapidProperties vapid) {
        this.pushSubscriptions = pushSubscriptions;
        this.userService = userService;
        this.vapid = vapid;
    }

    @GetMapping("/vapid-public-key")
    ResponseEntity<Map<String, String>> vapidPublicKey() {
        if (!vapid.isEnabled()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("publicKey", vapid.getPublicKey()));
    }

    @PostMapping("/subscribe")
    ResponseEntity<?> subscribe(@RequestBody @Valid SubscribeRequest request,
                                Principal principal,
                                HttpServletRequest servletRequest) {
        if (!vapid.isEnabled()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        User user = userService.requireByEmail(principal.getName());
        String ua = truncate(servletRequest.getHeader("User-Agent"), 500);
        try {
            pushSubscriptions.upsert(user, request.endpoint(), request.p256dhKey(),
                    request.authKey(), ua);
        } catch (PushSubscriptionService.SubscriptionLimitReachedException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "subscription-limit-reached"));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("ok", true));
    }

    @DeleteMapping("/subscribe")
    ResponseEntity<Map<String, Boolean>> unsubscribe(@RequestBody @Valid UnsubscribeRequest request,
                                                    Principal principal) {
        User user = userService.requireByEmail(principal.getName());
        boolean removed = pushSubscriptions.remove(user, request.endpoint());
        return ResponseEntity.ok(Map.of("removed", removed));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> onValidationError(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", "validation-failed"));
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() > max ? s.substring(0, max) : s;
    }

    record SubscribeRequest(
            @NotBlank @Size(max = 2000) String endpoint,
            @NotBlank @Size(max = 255) String p256dhKey,
            @NotBlank @Size(max = 255) String authKey
    ) {
    }

    record UnsubscribeRequest(
            @NotBlank @Size(max = 2000) String endpoint
    ) {
    }
}
