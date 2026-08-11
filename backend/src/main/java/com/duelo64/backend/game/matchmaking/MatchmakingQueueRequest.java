package com.duelo64.backend.game.matchmaking;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record MatchmakingQueueRequest(
        @Min(1) @Max(60) int timeControlMinutes) {
}
