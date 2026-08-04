package com.duelo64.backend.game.checkers.domain;

public record BoardPosition(int row, int column) {

    public boolean isInsideBoard() {
        return row >= 0 && row < 8 && column >= 0 && column < 8;
    }
}
