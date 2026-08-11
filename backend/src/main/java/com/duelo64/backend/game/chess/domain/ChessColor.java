package com.duelo64.backend.game.chess.domain;

public enum ChessColor {
    WHITE,
    BLACK;

    public ChessColor opponent() {
        return this == WHITE ? BLACK : WHITE;
    }
}
