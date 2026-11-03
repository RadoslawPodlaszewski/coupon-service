package com.example.coupons.application.port;

public class ClientCountryUnavailableException extends RuntimeException {

    public ClientCountryUnavailableException(String message) {
        super(message);
    }

    public ClientCountryUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
