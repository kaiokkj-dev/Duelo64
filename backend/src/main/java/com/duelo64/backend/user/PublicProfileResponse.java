package com.duelo64.backend.user;

import java.util.List;
import java.util.UUID;

import com.duelo64.backend.game.stats.PlayerStatsResponse;

public record PublicProfileResponse(
        UUID userId,
        String nickname,
        String avatarUrl,
        int rating,
        long gamesPlayed,
        long wins,
        long losses,
        long draws,
        double winRate,
        List<PublicMatchHistoryItemResponse> recentMatches) {

    public static PublicProfileResponse from(
            User user,
            PlayerStatsResponse stats,
            List<PublicMatchHistoryItemResponse> recentMatches) {
        return new PublicProfileResponse(
                user.getId(),
                user.getNickname(),
                user.getAvatarUrl(),
                stats.rating(),
                stats.gamesPlayed(),
                stats.wins(),
                stats.losses(),
                stats.draws(),
                stats.winRate(),
                List.copyOf(recentMatches));
    }
}
