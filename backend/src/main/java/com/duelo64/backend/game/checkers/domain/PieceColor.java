package com.duelo64.backend.game.checkers.domain;

public enum PieceColor {
    WHITE,
    BLACK;

    public PieceColor opponent() {
        return this == WHITE ? BLACK : WHITE;
    }
}
