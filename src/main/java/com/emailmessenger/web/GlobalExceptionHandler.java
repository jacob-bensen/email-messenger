package com.emailmessenger.web;

import com.emailmessenger.email.EmailImportException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.NoSuchElementException;

@ControllerAdvice
class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NoResourceFoundException.class)
    String notFound(Model model, HttpServletResponse response) {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        model.addAttribute("status", 404);
        model.addAttribute("message", "The page you were looking for doesn't exist.");
        return "error";
    }

    @ExceptionHandler(NoSuchElementException.class)
    String resourceMissing(Model model, HttpServletResponse response) {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        model.addAttribute("status", 404);
        model.addAttribute("message", "The requested item was not found.");
        return "error";
    }

    @ExceptionHandler({MailException.class, EmailImportException.class})
    String mailError(Exception ex, HttpServletRequest request, Model model, HttpServletResponse response) {
        log.warn("Mail operation failed for {}: {}", request.getRequestURI(), ex.getMessage());
        response.setStatus(HttpStatus.BAD_GATEWAY.value());
        model.addAttribute("status", 502);
        model.addAttribute("message",
                "Could not connect to the mail server. Please check your mailbox settings and try again.");
        return "error";
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    String dataConflict(DataIntegrityViolationException ex, HttpServletRequest request,
                        Model model, HttpServletResponse response) {
        log.warn("Data integrity violation for {}: {}", request.getRequestURI(), ex.getMessage());
        response.setStatus(HttpStatus.CONFLICT.value());
        model.addAttribute("status", 409);
        model.addAttribute("message", "A conflict occurred while saving your data. Please try again.");
        return "error";
    }

    @ExceptionHandler(Exception.class)
    String serverError(Exception ex, HttpServletRequest request,
                       Model model, HttpServletResponse response) {
        log.error("Unhandled exception for {}", request.getRequestURI(), ex);
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        model.addAttribute("status", 500);
        model.addAttribute("message", "Something went wrong on our end. Please try again in a moment.");
        return "error";
    }
}
