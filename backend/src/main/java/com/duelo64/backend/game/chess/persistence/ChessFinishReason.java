package com.duelo64.backend.game.chess.persistence;

public enum ChessFinishReason {
    CHECKMATE,
    STALEMATE,
    DRAW_FIFTY_MOVE_RULE,
    DRAW_REPETITION,
    DRAW_INSUFFICIENT_MATERIAL,
    TIMEOUT,
    RESIGNATION,
    DRAW_AGREEMENT
}
