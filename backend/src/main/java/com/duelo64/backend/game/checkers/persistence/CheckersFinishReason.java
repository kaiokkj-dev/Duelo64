package com.duelo64.backend.game.checkers.persistence;

public enum CheckersFinishReason {
    TIMEOUT,
    NO_PIECES,
    NO_LEGAL_MOVES,
    RESIGNATION,
    DRAW_AGREEMENT,
    DRAW_REPETITION,
    DRAW_MOVE_LIMIT
}
