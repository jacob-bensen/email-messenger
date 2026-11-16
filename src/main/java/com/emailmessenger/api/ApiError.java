package com.emailmessenger.api;

/** The docs/API.md §3 error envelope: {@code {"error": {"code", "message"}}}. */
record ApiError(Body error) {

    record Body(String code, String message) {}

    static ApiError of(String code, String message) {
        return new ApiError(new Body(code, message));
    }
}
