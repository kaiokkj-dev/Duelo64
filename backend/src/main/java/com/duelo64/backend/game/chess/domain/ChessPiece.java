package com.duelo64.backend.game.chess.domain;

import java.util.Objects;

public record ChessPiece(ChessColor color, ChessPieceType type) {
    public ChessPiece {
        Objects.requireNonNull(color, "A cor da peca e obrigatoria.");
        Objects.requireNonNull(type, "O tipo da peca e obrigatorio.");
    }
}
