package com.duelo64.backend.user;

import java.time.Instant;
import java.util.UUID;

import com.duelo64.backend.game.match.GameMatch;
import com.duelo64.backend.game.match.MatchHistoryItemResponse;

public record PublicMatchHistoryItemResponse(
        UUID id,
        String gameType,
        String matchType,
        String result,
        String finishReason,
        String playerColor,
        UUID opponentId,
        String opponentNickname,
        String opponentAvatarUrl,
        int timeControlMinutes,
        Instant finishedAt,
        int moveCount,
        Integer ratingChange) {

    public static PublicMatchHistoryItemResponse from(GameMatch match, UUID userId) {
        MatchHistoryItemResponse item = MatchHistoryItemResponse.from(match, userId);
        return new PublicMatchHistoryItemResponse(
                item.id(),
                item.gameType(),
                item.matchType(),
                item.result(),
                item.finishReason(),
                item.playerColor(),
                item.opponentId(),
                item.opponentNickname(),
                item.opponentAvatarUrl(),
                item.timeControlMinutes(),
                item.finishedAt(),
                item.moveCount(),
                item.ratingChange());
    }
}
