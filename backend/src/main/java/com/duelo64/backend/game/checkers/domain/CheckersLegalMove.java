package com.duelo64.backend.game.checkers.domain;

public record CheckersLegalMove(
        BoardPosition destination,
        boolean capture) {
}
