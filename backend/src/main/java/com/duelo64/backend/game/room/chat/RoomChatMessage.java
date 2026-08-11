package com.duelo64.backend.game.room.chat;

import java.time.Instant;
import java.util.UUID;

public record RoomChatMessage(
        UUID userId,
        String nickname,
        String text,
        Instant timestamp) {
}
