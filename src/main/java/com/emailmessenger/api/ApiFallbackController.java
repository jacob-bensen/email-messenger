package com.emailmessenger.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Catches {@code /api/**} paths no endpoint claims, so a client calling a
 * misspelled or not-yet-deployed route gets the JSON error envelope instead
 * of the website's HTML 404 page. Spring picks the most specific mapping, so
 * real endpoints always win over this pattern.
 */
@RestController
class ApiFallbackController {

    @RequestMapping("/api/**")
    ResponseEntity<ApiError> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of("not_found", "No such API endpoint."));
    }
}
