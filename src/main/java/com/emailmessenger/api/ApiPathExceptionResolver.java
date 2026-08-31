package com.emailmessenger.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

// A package-scoped @RestControllerAdvice does not apply here because
// HttpRequestMethodNotSupportedException and NoResourceFoundException are
// thrown before a specific controller class is resolved. A path-checking
// HandlerExceptionResolver at HIGHEST_PRECEDENCE catches every exception on
// /api/** and returns the JSON shape defined in docs/API.md; requests
// outside /api/** fall through to the HTML web/GlobalExceptionHandler.
@Component
class ApiPathExceptionResolver implements HandlerExceptionResolver, Ordered {

    private static final Logger log = LoggerFactory.getLogger(ApiPathExceptionResolver.class);
    private static final String API_PREFIX = "/api/";

    private final ObjectMapper mapper;

    ApiPathExceptionResolver(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public ModelAndView resolveException(HttpServletRequest request,
                                         HttpServletResponse response,
                                         Object handler,
                                         Exception ex) {
        String uri = request.getRequestURI();
        if (uri == null || !uri.startsWith(API_PREFIX)) {
            return null;
        }

        int status;
        String code;
        String message;
        if (ex instanceof HttpRequestMethodNotSupportedException e) {
            status = 405;
            code = "METHOD_NOT_ALLOWED";
            message = e.getMessage();
        } else if (ex instanceof NoResourceFoundException) {
            status = 404;
            code = "NOT_FOUND";
            message = "No API endpoint at " + uri;
        } else if (ex instanceof IllegalArgumentException) {
            status = 400;
            code = "BAD_REQUEST";
            message = ex.getMessage();
        } else {
            status = 500;
            code = "INTERNAL_ERROR";
            message = "The server hit an unexpected error. Please try again.";
            log.error("Unhandled /api/** exception at {}", uri, ex);
        }

        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try {
            byte[] body = mapper.writeValueAsBytes(ApiError.of(code, message));
            response.getOutputStream().write(body);
            response.getOutputStream().flush();
        } catch (IOException ioe) {
            log.warn("Failed to write /api/** error response for {}", uri, ioe);
        }
        return new ModelAndView();
    }
}
