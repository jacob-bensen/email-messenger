package com.emailmessenger.api;

/** The single error envelope every {@code /api/v1} failure uses: {@code {"error":{"code","message"}}}. */
record ApiError(Detail error) {

    record Detail(String code, String message) {}

    static ApiError of(String code, String message) {
        return new ApiError(new Detail(code, message));
    }
}
