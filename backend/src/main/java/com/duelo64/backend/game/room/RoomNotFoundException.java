package com.duelo64.backend.game.room;

public class RoomNotFoundException extends RuntimeException {

    public RoomNotFoundException() {
        super("Sala nao encontrada.");
    }
}
