package com.duelo64.backend.game.room;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CreateRoomRequest(
        @Min(3) @Max(30) int timeControlMinutes) {
}
