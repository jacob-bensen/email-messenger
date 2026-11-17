package com.emailmessenger.api;

import com.emailmessenger.billing.PlanLimitExceededException;
import com.emailmessenger.billing.PlanLimitKind;
import com.emailmessenger.domain.Plan;
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

import java.util.Locale;

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

    @ExceptionHandler(PlanLimitExceededException.class)
    ResponseEntity<ApiError> planLimit(PlanLimitExceededException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.of("plan_limit_reached", planLimitMessage(ex)));
    }

    static String planLimitMessage(PlanLimitExceededException ex) {
        String plan = planName(ex.getCurrentPlan());
        String what = switch (ex.getKind()) {
            case MAILBOX_COUNT -> ex.getLimit() == 1 ? "1 mailbox" : ex.getLimit() + " mailboxes";
            case THREAD_COUNT -> ex.getLimit() + " email threads";
            case SAVED_SEARCH_COUNT -> ex.getLimit() + " saved searches";
        };
        return "Your " + plan + " plan includes " + what + ". Upgrade on the website to "
                + (ex.getKind() == PlanLimitKind.MAILBOX_COUNT ? "connect more." : "get more.");
    }

    private static String planName(Plan plan) {
        String lower = plan.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ApiError> invalid(Exception ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of("validation_failed", "The request body is missing or malformed."));
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
