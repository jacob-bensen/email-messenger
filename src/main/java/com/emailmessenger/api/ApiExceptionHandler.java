package com.emailmessenger.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * JSON error envelope for every {@code /api} controller. Ordered ahead of the
 * website's {@code GlobalExceptionHandler}, which renders HTML error pages.
 * Unknown paths and wrong methods are answered by {@code RoutingErrorHandler}.
 */
@RestControllerAdvice(basePackageClasses = ApiExceptionHandler.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> api(ApiException ex) {
        return ResponseEntity.status(ex.status()).body(ApiError.of(ex.code(), ex.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of("validation_failed", "The request body is missing or malformed."));
    }

    /**
     * Our own constraint messages are full sentences ("Port must be between 1
     * and 65535."), safe to show; Bean Validation's defaults ("must not be
     * blank") are not, so those name the field instead.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalid(MethodArgumentNotValidException ex) {
        FieldError field = ex.getBindingResult().getFieldError();
        String message;
        if (field == null) {
            message = "The request body is missing or malformed.";
        } else if (field.getDefaultMessage() != null && field.getDefaultMessage().endsWith(".")) {
            message = field.getDefaultMessage();
        } else {
            message = "Invalid value for '" + field.getField() + "'.";
        }
        return ResponseEntity.badRequest().body(ApiError.of("validation_failed", message));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> badParameter(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of("validation_failed", "Invalid value for '" + ex.getName() + "'."));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception ex) {
        log.error("Unhandled API exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of("internal_error", "Something went wrong on our end."));
    }
}
