package com.duelo64.backend.game.stats;

import com.duelo64.backend.game.room.GameType;

public record PlayerStatsResponse(
        GameType gameType,
        int rating,
        long gamesPlayed,
        long wins,
        long losses,
        long draws,
        double winRate) {
}
