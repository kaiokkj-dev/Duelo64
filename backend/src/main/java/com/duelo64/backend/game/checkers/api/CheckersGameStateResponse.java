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
        String status,
        String winnerColor,
        String finishReason) {

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
                state.getRoom().getStatus().name(),
                state.getWinnerColor() == null ? null : state.getWinnerColor().name(),
                state.getFinishReason() == null ? null : state.getFinishReason().name());
    }
}
