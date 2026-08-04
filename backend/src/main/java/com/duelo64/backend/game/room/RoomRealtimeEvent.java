package com.duelo64.backend.game.room;

import java.time.Instant;

public record RoomRealtimeEvent(
        String type,
        String roomCode,
        RoomStatus status,
        Instant occurredAt) {

    public static RoomRealtimeEvent roomCreated(GameRoom room) {
        return new RoomRealtimeEvent(
                "ROOM_CREATED",
                room.getCode(),
                room.getStatus(),
                Instant.now());
    }

    public static RoomRealtimeEvent playerJoined(GameRoom room) {
        return new RoomRealtimeEvent(
                "PLAYER_JOINED",
                room.getCode(),
                room.getStatus(),
                Instant.now());
    }

    public static RoomRealtimeEvent gameStateUpdated(GameRoom room) {
        return new RoomRealtimeEvent(
                "GAME_STATE_UPDATED",
                room.getCode(),
                room.getStatus(),
                Instant.now());
    }
}
