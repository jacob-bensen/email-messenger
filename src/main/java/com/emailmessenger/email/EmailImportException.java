package com.emailmessenger.email;

public class EmailImportException extends RuntimeException {

    EmailImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
