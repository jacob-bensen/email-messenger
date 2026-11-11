package com.emailmessenger.api;

/** The error envelope every non-2xx {@code /api/v1} response carries. See docs/API.md. */
record ApiError(Detail error) {

    record Detail(String code, String message) {}

    static ApiError of(String code, String message) {
        return new ApiError(new Detail(code, message));
    }
}
