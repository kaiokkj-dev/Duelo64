package com.duelo64.backend.game.room;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RoomRealtimeEvent(
        String type,
        String roomCode,
        RoomStatus status,
        UUID userId,
        List<UUID> connectedUserIds,
        UUID rematchRequestedByUserId,
        String newRoomCode,
        Instant occurredAt) {

    public static RoomRealtimeEvent roomCreated(GameRoom room) {
        return new RoomRealtimeEvent(
                "ROOM_CREATED",
                room.getCode(),
                room.getStatus(),
                null,
                List.of(),
                null,
                null,
                Instant.now());
    }

    public static RoomRealtimeEvent playerJoined(GameRoom room) {
        return new RoomRealtimeEvent(
                "PLAYER_JOINED",
                room.getCode(),
                room.getStatus(),
                null,
                List.of(),
                null,
                null,
                Instant.now());
    }

    public static RoomRealtimeEvent gameStateUpdated(GameRoom room) {
        return new RoomRealtimeEvent(
                "GAME_STATE_UPDATED",
                room.getCode(),
                room.getStatus(),
                null,
                List.of(),
                null,
                null,
                Instant.now());
    }

    public static RoomRealtimeEvent playerConnected(GameRoom room, UUID userId) {
        return presenceEvent("PLAYER_CONNECTED", room, userId, List.of());
    }

    public static RoomRealtimeEvent playerDisconnected(GameRoom room, UUID userId) {
        return presenceEvent("PLAYER_DISCONNECTED", room, userId, List.of());
    }

    public static RoomRealtimeEvent presenceSnapshot(GameRoom room, List<UUID> connectedUserIds) {
        return presenceEvent("PRESENCE_SNAPSHOT", room, null, connectedUserIds);
    }

    public static RoomRealtimeEvent rematchRequested(GameRoom room) {
        return rematchEvent("REMATCH_REQUESTED", room, null);
    }

    public static RoomRealtimeEvent rematchDeclined(GameRoom room) {
        return rematchEvent("REMATCH_DECLINED", room, null);
    }

    public static RoomRealtimeEvent rematchAccepted(GameRoom room, String newRoomCode) {
        return rematchEvent("REMATCH_ACCEPTED", room, newRoomCode);
    }

    private static RoomRealtimeEvent rematchEvent(String type, GameRoom room, String newRoomCode) {
        return new RoomRealtimeEvent(
                type,
                room.getCode(),
                room.getStatus(),
                null,
                List.of(),
                room.getRematchRequestedByUserId(),
                newRoomCode,
                Instant.now());
    }

    private static RoomRealtimeEvent presenceEvent(
            String type,
            GameRoom room,
            UUID userId,
            List<UUID> connectedUserIds) {
        return new RoomRealtimeEvent(
                type,
                room.getCode(),
                room.getStatus(),
                userId,
                List.copyOf(connectedUserIds),
                null,
                null,
                Instant.now());
    }
}
