package com.duelo64.backend.game.matchmaking;

import com.duelo64.backend.game.room.RoomPlayerResponse;

public record MatchFoundResponse(
        String roomCode,
        String color,
        RoomPlayerResponse opponent,
        int timeControlMinutes) {
}
