package com.duelo64.backend.game.room.chat;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class RoomChatRateLimiterTest {

    @Test
    void blocksSeventhMessageInsideWindow() {
        RoomChatRateLimiter limiter = new RoomChatRateLimiter(
                Clock.fixed(Instant.parse("2026-08-10T18:00:00Z"), ZoneOffset.UTC));
        UUID userId = UUID.randomUUID();

        for (int index = 0; index < RoomChatRateLimiter.MAX_MESSAGES; index++) {
            assertThat(limiter.tryAcquire(userId)).isTrue();
        }
        assertThat(limiter.tryAcquire(userId)).isFalse();
    }
}
