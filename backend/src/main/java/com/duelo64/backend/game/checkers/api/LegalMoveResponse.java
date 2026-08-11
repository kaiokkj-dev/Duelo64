package com.duelo64.backend.game.checkers.api;

import com.duelo64.backend.game.checkers.domain.CheckersLegalMove;

public record LegalMoveResponse(
        int toRow,
        int toColumn,
        boolean capture) {

    public static LegalMoveResponse from(CheckersLegalMove move) {
        return new LegalMoveResponse(
                move.destination().row(),
                move.destination().column(),
                move.capture());
    }
}
