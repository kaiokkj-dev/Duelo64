package com.duelo64.backend.game.checkers.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MovePieceRequest(
        @NotNull @Min(0) @Max(7) Integer fromRow,
        @NotNull @Min(0) @Max(7) Integer fromColumn,
        @NotNull @Min(0) @Max(7) Integer toRow,
        @NotNull @Min(0) @Max(7) Integer toColumn) {
}
