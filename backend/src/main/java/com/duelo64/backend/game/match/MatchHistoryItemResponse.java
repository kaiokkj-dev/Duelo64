package com.duelo64.backend.game.match;

import java.time.Instant;
import java.util.UUID;
import com.duelo64.backend.user.User;

public record MatchHistoryItemResponse(
        UUID id, String gameType, String matchType, String roomCode, String result, String finishReason,
        String playerColor, UUID opponentId, String opponentNickname, String opponentAvatarUrl,
        int timeControlMinutes, Instant startedAt, Instant finishedAt, long durationMillis, int moveCount,
        Integer ratingChange) {

    public static MatchHistoryItemResponse from(GameMatch match, UUID userId) {
        boolean playedWhite = match.getWhitePlayer().getId().equals(userId);
        User opponent = playedWhite ? match.getBlackPlayer() : match.getWhitePlayer();
        String personalResult = match.getResult() == MatchResult.DRAW ? "DRAW"
                : match.getWinner().getId().equals(userId) ? "WIN" : "LOSS";
        Integer ratingChange = playedWhite
                ? difference(match.getWhiteRatingBefore(), match.getWhiteRatingAfter())
                : difference(match.getBlackRatingBefore(), match.getBlackRatingAfter());
        return new MatchHistoryItemResponse(
                match.getId(), match.getGameType().name(), match.getMatchType().name(), match.getRoomCode(), personalResult,
                match.getFinishReason(), playedWhite ? "WHITE" : "BLACK",
                opponent.getId(), opponent.getNickname(), opponent.getAvatarUrl(),
                match.getTimeControlMinutes(), match.getStartedAt(), match.getFinishedAt(),
                match.getDurationMillis(), match.getMoveCount(), ratingChange);
    }

    private static Integer difference(Integer before, Integer after) {
        return before == null || after == null ? null : after - before;
    }
}
