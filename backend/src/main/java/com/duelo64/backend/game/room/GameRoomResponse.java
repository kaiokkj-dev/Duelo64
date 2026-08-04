package com.duelo64.backend.game.room;

import java.time.Instant;
import java.util.UUID;

public record GameRoomResponse(
        UUID id,
        String code,
        GameType gameType,
        RoomType roomType,
        RoomStatus status,
        int timeControlMinutes,
        RoomPlayerResponse host,
        RoomPlayerResponse guest,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt) {

    public static GameRoomResponse from(GameRoom room) {
        return new GameRoomResponse(
                room.getId(),
                room.getCode(),
                room.getGameType(),
                room.getRoomType(),
                room.getStatus(),
                room.getTimeControlMinutes(),
                RoomPlayerResponse.from(room.getHost()),
                RoomPlayerResponse.from(room.getGuest()),
                room.getCreatedAt(),
                room.getStartedAt(),
                room.getFinishedAt());
    }
}
