package com.emailmessenger.api;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JSON error responses for the mobile-facing REST controllers. Scoped to
 * {@code com.emailmessenger.api} so the site-wide
 * {@link com.emailmessenger.web.GlobalExceptionHandler} (which renders
 * the HTML error page) never fires for these routes. Higher precedence
 * so this advice wins over the global one for API packages.
 */
@RestControllerAdvice(basePackages = "com.emailmessenger.api")
@Order(Ordered.HIGHEST_PRECEDENCE)
class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String, Object>> handleStatus(ResponseStatusException ex) {
        HttpStatusCode status = ex.getStatusCode();
        return ResponseEntity.status(status).body(body(status, ex.getReason()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, Object>> handleBadArg(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(body(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }

    private static Map<String, Object> body(HttpStatusCode status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status instanceof HttpStatus hs ? hs.getReasonPhrase() : "Error");
        body.put("message", message == null ? "" : message);
        return body;
    }
}
