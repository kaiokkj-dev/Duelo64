package com.duelo64.backend.game.checkers.domain;

import java.util.Objects;

public final class Piece {

    private final PieceColor color;
    private final boolean king;

    public Piece(PieceColor color, boolean king) {
        this.color = Objects.requireNonNull(color);
        this.king = king;
    }

    public PieceColor getColor() {
        return color;
    }

    public boolean isKing() {
        return king;
    }

    public Piece promote() {
        if (king) {
            return this;
        }

        return new Piece(color, true);
    }
}
