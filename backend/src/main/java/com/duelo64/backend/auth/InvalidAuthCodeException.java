package com.duelo64.backend.auth;

public class InvalidAuthCodeException extends RuntimeException {

    public InvalidAuthCodeException(String message) {
        super(message);
    }
}