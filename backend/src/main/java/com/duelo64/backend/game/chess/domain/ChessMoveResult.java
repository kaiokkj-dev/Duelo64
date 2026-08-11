package com.duelo64.backend.game.chess.domain;

public record ChessMoveResult(
        ChessBoard board,
        boolean capture,
        boolean pawnMove,
        ChessCastlingRights castlingRights,
        ChessPosition enPassantTarget) {
}
