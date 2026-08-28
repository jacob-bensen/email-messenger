package com.emailmessenger.web.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * The JSON error shape documented in {@code docs/API.md} §Errors:
 * <pre>{@code {"error": "<slug>", "message": "…", "details": {…}}}</pre>
 *
 * <p>{@code message} and {@code details} are omitted from the wire when
 * unset — clients only need to switch on {@code error}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(String error, String message, Map<String, Object> details) {

    public static ApiErrorResponse of(String error) {
        return new ApiErrorResponse(error, null, null);
    }

    public static ApiErrorResponse of(String error, String message) {
        return new ApiErrorResponse(error, message, null);
    }
}
