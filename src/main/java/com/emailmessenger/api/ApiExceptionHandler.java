package com.emailmessenger.api;

import com.emailmessenger.auth.ApiTokenService.InvalidCredentialsException;
import com.emailmessenger.auth.ApiTokenService.LoginThrottledException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.NoSuchElementException;

// Ordered ahead of web.GlobalExceptionHandler, which renders the HTML error
// page and would otherwise claim these exceptions for API controllers too.
@RestControllerAdvice(basePackages = "com.emailmessenger.api")
@Order(Ordered.HIGHEST_PRECEDENCE)
class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ApiError> invalidCredentials() {
        return error(HttpStatus.UNAUTHORIZED, "invalid_credentials", "Incorrect email or password.");
    }

    @ExceptionHandler(LoginThrottledException.class)
    ResponseEntity<ApiError> throttled() {
        return error(HttpStatus.TOO_MANY_REQUESTS, "too_many_attempts",
                "Too many sign-in attempts. Try again later.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .orElse("The request was invalid.");
        return error(HttpStatus.BAD_REQUEST, "validation_failed", message);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class})
    ResponseEntity<ApiError> malformed() {
        return error(HttpStatus.BAD_REQUEST, "bad_request", "The request was malformed.");
    }

    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<ApiError> notFound() {
        return error(HttpStatus.NOT_FOUND, "not_found", "The requested item was not found.");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> serverError(Exception ex) {
        log.error("Unhandled API exception", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error",
                "Something went wrong on our end.");
    }

    private static ResponseEntity<ApiError> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ApiError.of(code, message));
    }
}
