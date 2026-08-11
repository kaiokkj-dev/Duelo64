package com.duelo64.backend.game.stats;

import java.util.UUID;

public record CheckersRankingEntryResponse(
        long position,
        UUID userId,
        String nickname,
        String avatarUrl,
        int rating,
        long rankedGames,
        long rankedWins,
        long rankedLosses,
        long rankedDraws,
        double rankedWinRate) {
}
