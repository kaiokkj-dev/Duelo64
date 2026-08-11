package com.duelo64.backend.game.matchmaking;

import java.time.Instant;
import java.util.UUID;

import com.duelo64.backend.game.room.GameType;

record MatchmakingQueueEntry(
        UUID userId,
        GameType gameType,
        int timeControlMinutes,
        int rating,
        Instant queuedAt) {
}
