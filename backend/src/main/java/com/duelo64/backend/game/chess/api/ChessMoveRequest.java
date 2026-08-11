package com.duelo64.backend.game.chess.api;

import com.duelo64.backend.game.chess.domain.ChessPieceType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ChessMoveRequest(
        @Min(0) @Max(7) int fromRow,
        @Min(0) @Max(7) int fromColumn,
        @Min(0) @Max(7) int toRow,
        @Min(0) @Max(7) int toColumn,
        ChessPieceType promotion) {}
