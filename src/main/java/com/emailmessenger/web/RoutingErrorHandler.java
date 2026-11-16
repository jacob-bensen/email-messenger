package com.emailmessenger.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

/**
 * Errors raised before any controller is chosen (unknown path, wrong method).
 * Package-scoped advice never sees these because there is no handler type to
 * match, so this one decides by path: {@code /api/**} gets the docs/API.md
 * §5.1 JSON envelope, everything else the website's HTML error page.
 */
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class RoutingErrorHandler {

    @ExceptionHandler(NoResourceFoundException.class)
    Object notFound(HttpServletRequest request, HttpServletResponse response) {
        return respond(request, response, HttpStatus.NOT_FOUND, "not_found",
                "The page you were looking for doesn't exist.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    Object methodNotAllowed(HttpServletRequest request, HttpServletResponse response) {
        return respond(request, response, HttpStatus.METHOD_NOT_ALLOWED, "method_not_allowed",
                "That action isn't supported here.");
    }

    private static Object respond(HttpServletRequest request, HttpServletResponse response,
                                  HttpStatus status, String code, String message) {
        if (request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {
            return ResponseEntity.status(status)
                    .body(Map.of("error", Map.of("code", code, "message", message)));
        }
        response.setStatus(status.value());
        ModelAndView page = new ModelAndView("error");
        page.addObject("status", status.value());
        page.addObject("message", message);
        return page;
    }
}
