package com.emailmessenger.api;

import org.springframework.http.HttpStatus;

/** A failure with a contract-defined status + error code (see docs/API.md §Errors). */
class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    HttpStatus status() { return status; }
    String code() { return code; }

    static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, "not_found", message);
    }
}
