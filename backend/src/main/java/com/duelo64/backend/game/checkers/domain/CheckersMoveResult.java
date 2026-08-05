package com.duelo64.backend.game.checkers.domain;

public record CheckersMoveResult(
        CheckersBoard board,
        boolean capture,
        boolean mustContinueCapture,
        BoardPosition landingPosition) {
}
