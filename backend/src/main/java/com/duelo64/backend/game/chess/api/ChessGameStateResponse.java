package com.duelo64.backend.game.chess.api;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.duelo64.backend.game.chess.domain.*;
import com.duelo64.backend.game.chess.persistence.ChessGameState;

public record ChessGameStateResponse(
        String roomCode, List<List<String>> board, String fen, String currentTurn,
        int moveCount, long whiteRemainingMillis, long blackRemainingMillis,
        Instant turnStartedAt, Instant serverTime, String status,
        String winnerColor, String loserColor, String finishReason, Instant finishedAt,
        boolean drawOfferPending, String drawOfferedByColor, boolean inCheck) {

    public static ChessGameStateResponse from(ChessGameState state) {
        Instant now = Instant.now();
        ChessFen fen = state.chessFen();
        return new ChessGameStateResponse(
                state.getRoom().getCode(), matrix(fen.board()), fen.notation(), fen.activeColor().name(),
                state.getMoveCount(), state.calculateWhiteRemainingMillis(now), state.calculateBlackRemainingMillis(now),
                state.getTurnStartedAt(), now, state.getRoom().getStatus().name(),
                name(state.getWinnerColor()), name(state.getLoserColor()),
                state.getFinishReason() == null ? null : state.getFinishReason().name(), state.getFinishedAt(),
                state.hasPendingDrawOffer(), name(state.getDrawOfferedByColor()),
                new ChessRules().isInCheck(fen.board(), fen.activeColor()));
    }

    private static List<List<String>> matrix(ChessBoard board) {
        List<List<String>> rows = new ArrayList<>();
        for (int row = 0; row < 8; row++) {
            List<String> cells = new ArrayList<>();
            for (int column = 0; column < 8; column++) {
                ChessPiece piece = board.pieceAt(new ChessPosition(row, column));
                cells.add(piece == null ? null : piece.color().name() + "_" + piece.type().name());
            }
            rows.add(cells);
        }
        return rows;
    }

    private static String name(Enum<?> value) { return value == null ? null : value.name(); }
}
