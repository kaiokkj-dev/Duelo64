package com.duelo64.backend.game.stats;

import java.util.UUID;

public interface CheckersRankingProjection {
    UUID getUserId();
    String getNickname();
    String getAvatarUrl();
    int getRating();
    long getRankedGames();
    long getRankedWins();
    long getRankedLosses();
    long getRankedDraws();
}
