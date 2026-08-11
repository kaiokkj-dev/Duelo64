package com.duelo64.backend.game.chess.domain;

public record ChessPosition(int row, int column) {
    public boolean isInsideBoard() {
        return row >= 0 && row < ChessBoard.SIZE && column >= 0 && column < ChessBoard.SIZE;
    }
}
