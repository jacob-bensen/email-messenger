package com.emailmessenger.api;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * JSON errors for the REST API. Ordered ahead of the site-wide
 * {@code GlobalExceptionHandler}, which would otherwise render the HTML
 * error page for exceptions thrown by API controllers.
 */
@RestControllerAdvice(basePackages = "com.emailmessenger.api")
@Order(Ordered.HIGHEST_PRECEDENCE)
class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<ApiError> badCredentials() {
        return respond(HttpStatus.UNAUTHORIZED, "invalid_credentials", "Invalid email or password.");
    }

    @ExceptionHandler(LockedException.class)
    ResponseEntity<ApiError> locked() {
        return respond(HttpStatus.TOO_MANY_REQUESTS, "too_many_attempts",
                "Too many failed sign-in attempts. Try again later.");
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMediaTypeNotSupportedException.class})
    ResponseEntity<ApiError> invalidRequest() {
        return respond(HttpStatus.BAD_REQUEST, "invalid_request", "The request was malformed.");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> serverError(Exception ex, HttpServletRequest request) {
        log.error("Unhandled API exception for {}", request.getRequestURI(), ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error",
                "Something went wrong on our end. Please try again in a moment.");
    }

    private static ResponseEntity<ApiError> respond(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ApiError.of(code, message));
    }
}
