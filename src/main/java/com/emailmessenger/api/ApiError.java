package com.emailmessenger.api;

record ApiError(Body error) {

    static ApiError of(String code, String message) {
        return new ApiError(new Body(code, message));
    }

    record Body(String code, String message) {
    }
}
