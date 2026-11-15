package com.emailmessenger.api;

import org.springframework.http.HttpStatus;

/** An error the API reports with the docs/API.md §3 envelope. */
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
}
