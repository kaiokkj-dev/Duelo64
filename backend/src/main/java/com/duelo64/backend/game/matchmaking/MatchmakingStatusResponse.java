package com.duelo64.backend.game.matchmaking;

import java.time.Instant;

public record MatchmakingStatusResponse(
        String status,
        Integer timeControlMinutes,
        Integer rating,
        Instant queuedAt,
        MatchFoundResponse match) {

    static MatchmakingStatusResponse idle() {
        return new MatchmakingStatusResponse("IDLE", null, null, null, null);
    }

    static MatchmakingStatusResponse queued(MatchmakingQueueEntry entry) {
        return new MatchmakingStatusResponse(
                "QUEUED", entry.timeControlMinutes(), entry.rating(), entry.queuedAt(), null);
    }

    static MatchmakingStatusResponse found(MatchFoundResponse match) {
        return new MatchmakingStatusResponse(
                "MATCH_FOUND", match.timeControlMinutes(), null, null, match);
    }
}
