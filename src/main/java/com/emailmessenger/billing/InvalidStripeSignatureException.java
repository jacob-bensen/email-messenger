package com.emailmessenger.billing;

public class InvalidStripeSignatureException extends RuntimeException {

    public InvalidStripeSignatureException(String message, Throwable cause) {
        super(message, cause);
    }
}
