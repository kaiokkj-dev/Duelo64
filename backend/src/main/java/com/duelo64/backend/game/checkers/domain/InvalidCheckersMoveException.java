package com.duelo64.backend.game.checkers.domain;

public class InvalidCheckersMoveException extends RuntimeException {

    public InvalidCheckersMoveException(String message) {
        super(message);
    }
}
