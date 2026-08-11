package com.duelo64.backend.game.room;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.duelo64.backend.user.User;

class GameRoomTest {

    @Test
    void finishShouldSetFinishedAtOnlyOnce() {
        GameRoom room = GameRoom.privateCheckers("ABC123", 10, mock(User.class));

        room.finish();
        Instant firstFinishedAt = room.getFinishedAt();
        room.finish();

        assertThat(room.getStatus()).isEqualTo(RoomStatus.FINISHED);
        assertThat(firstFinishedAt).isNotNull();
        assertThat(room.getFinishedAt()).isEqualTo(firstFinishedAt);
    }
}
