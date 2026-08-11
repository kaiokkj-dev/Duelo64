package com.duelo64.backend.game.checkers.api;

import java.time.Instant;
import java.util.List;

import com.duelo64.backend.game.checkers.domain.CheckersBoard;
import com.duelo64.backend.game.checkers.persistence.CheckersGameState;

public record CheckersGameStateResponse(
        String roomCode,
        List<List<String>> board,
        String currentTurn,
        int moveCount,
        boolean mustContinueCapture,
        Integer forcedCaptureRow,
        Integer forcedCaptureColumn,
        long whiteRemainingMillis,
        long blackRemainingMillis,
        Instant turnStartedAt,
        Instant serverTime,
        String status,
        String winnerColor,
        String loserColor,
        String finishReason,
        Instant finishedAt,
        boolean drawOfferPending,
        String drawOfferedByColor) {

    public static CheckersGameStateResponse from(CheckersGameState state) {
        CheckersBoard board = CheckersBoard.fromNotation(state.getBoardNotation());

        Instant now = Instant.now();
        long whiteRemainingMillis = state.calculateWhiteRemainingMillis(now);
        long blackRemainingMillis = state.calculateBlackRemainingMillis(now);

        return new CheckersGameStateResponse(
                state.getRoom().getCode(),
                board.toMatrix(),
                state.getCurrentTurn().name(),
                state.getMoveCount(),
                state.mustContinueCapture(),
                state.getForcedCaptureRow(),
                state.getForcedCaptureColumn(),
                whiteRemainingMillis,
                blackRemainingMillis,
                state.getTurnStartedAt(),
                now,
                state.getRoom().getStatus().name(),
                state.getWinnerColor() == null ? null : state.getWinnerColor().name(),
                state.getWinnerColor() == null ? null : state.getWinnerColor().opponent().name(),
                state.getFinishReason() == null ? null : state.getFinishReason().name(),
                state.getRoom().getFinishedAt(),
                state.hasPendingDrawOffer(),
                state.getDrawOfferedByColor() == null ? null : state.getDrawOfferedByColor().name());
    }
}
