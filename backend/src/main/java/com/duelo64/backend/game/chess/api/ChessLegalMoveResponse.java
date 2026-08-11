package com.duelo64.backend.game.chess.api;

import com.duelo64.backend.game.chess.domain.ChessLegalMove;

public record ChessLegalMoveResponse(int toRow, int toColumn, boolean capture, boolean promotionRequired) {
    public static ChessLegalMoveResponse from(ChessLegalMove move) {
        return new ChessLegalMoveResponse(move.toRow(), move.toColumn(), move.capture(), move.promotionRequired());
    }
}
