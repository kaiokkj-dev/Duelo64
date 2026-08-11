package com.duelo64.backend.game.chess.domain;

public class InvalidChessMoveException extends RuntimeException {
    public InvalidChessMoveException(String message) {
        super(message);
    }
}
