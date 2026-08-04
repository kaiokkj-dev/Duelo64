package com.duelo64.backend.game.room;

public class RoomUnavailableException extends RuntimeException {

    public RoomUnavailableException(String message) {
        super(message);
    }
}
