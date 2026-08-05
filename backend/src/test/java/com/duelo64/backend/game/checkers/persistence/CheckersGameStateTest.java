package com.duelo64.backend.game.checkers.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.duelo64.backend.game.checkers.domain.BoardPosition;
import com.duelo64.backend.game.checkers.domain.CheckersBoard;
import com.duelo64.backend.game.checkers.domain.CheckersMoveResult;
import com.duelo64.backend.game.checkers.domain.PieceColor;
import com.duelo64.backend.game.room.GameRoom;

class CheckersGameStateTest {

    @Test
    void turnShouldRemainDuringMultipleCaptureAndChangeWhenItEnds() {
        GameRoom room = mock(GameRoom.class);
        when(room.getTimeControlMinutes()).thenReturn(10);
        CheckersGameState state = CheckersGameState.start(room);
        Instant now = Instant.now();

        state.apply(new CheckersMoveResult(
                CheckersBoard.initial(), true, true, new BoardPosition(3, 2)), now);

        assertThat(state.getCurrentTurn()).isEqualTo(PieceColor.WHITE);
        assertThat(state.mustContinueCapture()).isTrue();
        assertThat(state.getForcedCaptureRow()).isEqualTo(3);
        assertThat(state.getForcedCaptureColumn()).isEqualTo(2);

        state.apply(new CheckersMoveResult(
                CheckersBoard.initial(), true, false, new BoardPosition(1, 4)), now);

        assertThat(state.getCurrentTurn()).isEqualTo(PieceColor.BLACK);
        assertThat(state.mustContinueCapture()).isFalse();
    }
}
