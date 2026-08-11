package com.duelo64.backend.game.checkers.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.duelo64.backend.game.checkers.domain.BoardPosition;
import com.duelo64.backend.game.checkers.domain.CheckersBoard;
import com.duelo64.backend.game.checkers.domain.CheckersMoveResult;
import com.duelo64.backend.game.checkers.domain.PieceColor;
import com.duelo64.backend.game.room.GameRoom;

class CheckersGameStateTest {

    @Test
    void clocksShouldStartWithRoomTimeAndRemainStoppedWhileWaiting() {
        GameRoom room = mock(GameRoom.class);
        when(room.getTimeControlMinutes()).thenReturn(10);
        CheckersGameState state = CheckersGameState.start(room);

        assertThat(state.getWhiteRemainingMillis()).isEqualTo(600_000L);
        assertThat(state.getBlackRemainingMillis()).isEqualTo(600_000L);
        assertThat(state.getTurnStartedAt()).isNull();
        assertThat(state.calculateWhiteRemainingMillis(Instant.now().plusSeconds(30))).isEqualTo(600_000L);
        assertThat(state.calculateBlackRemainingMillis(Instant.now().plusSeconds(30))).isEqualTo(600_000L);
    }

    @Test
    void onlyCurrentPlayerClockShouldLoseTime() {
        GameRoom room = mock(GameRoom.class);
        when(room.getTimeControlMinutes()).thenReturn(10);
        CheckersGameState state = CheckersGameState.start(room);
        Instant startedAt = Instant.parse("2026-08-10T12:00:00Z");
        state.startClock(startedAt);

        state.consumeCurrentTurnTime(startedAt.plusSeconds(2));

        assertThat(state.getWhiteRemainingMillis()).isEqualTo(598_000L);
        assertThat(state.getBlackRemainingMillis()).isEqualTo(600_000L);
    }

    @Test
    void clockShouldPassToOpponentAfterCompletedMove() {
        GameRoom room = mock(GameRoom.class);
        when(room.getTimeControlMinutes()).thenReturn(10);
        CheckersGameState state = CheckersGameState.start(room);
        Instant startedAt = Instant.parse("2026-08-10T12:00:00Z");
        Instant moveAt = startedAt.plusSeconds(2);
        state.startClock(startedAt);
        state.consumeCurrentTurnTime(moveAt);

        state.apply(new CheckersMoveResult(
                CheckersBoard.initial(), false, false, new BoardPosition(4, 1)), moveAt);

        assertThat(state.getCurrentTurn()).isEqualTo(PieceColor.BLACK);
        assertThat(state.getWhiteRemainingMillis()).isEqualTo(598_000L);
        assertThat(state.calculateBlackRemainingMillis(moveAt.plusSeconds(1))).isEqualTo(599_000L);
    }

    @Test
    void multipleCaptureShouldContinueSameClockWithoutResettingRemainingTime() {
        GameRoom room = mock(GameRoom.class);
        when(room.getTimeControlMinutes()).thenReturn(10);
        CheckersGameState state = CheckersGameState.start(room);
        Instant startedAt = Instant.parse("2026-08-10T12:00:00Z");
        Instant captureAt = startedAt.plusSeconds(2);
        state.startClock(startedAt);
        state.consumeCurrentTurnTime(captureAt);

        state.apply(new CheckersMoveResult(
                CheckersBoard.initial(), true, true, new BoardPosition(3, 2)), captureAt);

        assertThat(state.getCurrentTurn()).isEqualTo(PieceColor.WHITE);
        assertThat(state.getWhiteRemainingMillis()).isEqualTo(598_000L);
        assertThat(state.calculateWhiteRemainingMillis(captureAt.plusSeconds(1))).isEqualTo(597_000L);
    }

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

    @Test
    void finishedResultShouldNotBeOverwritten() {
        GameRoom room = mock(GameRoom.class);
        when(room.getTimeControlMinutes()).thenReturn(10);
        CheckersGameState state = CheckersGameState.start(room);

        state.finish(PieceColor.WHITE, CheckersFinishReason.NO_PIECES);
        state.finish(PieceColor.BLACK, CheckersFinishReason.NO_LEGAL_MOVES);

        assertThat(state.getWinnerColor()).isEqualTo(PieceColor.WHITE);
        assertThat(state.getFinishReason()).isEqualTo(CheckersFinishReason.NO_PIECES);
    }

    @Test
    void sameLegalPositionShouldBeCountedThreeTimes() {
        GameRoom room = mock(GameRoom.class);
        when(room.getTimeControlMinutes()).thenReturn(10);
        CheckersGameState state = CheckersGameState.start(room);

        assertThat(state.recordCurrentPosition()).isEqualTo(2);
        assertThat(state.recordCurrentPosition()).isEqualTo(3);
    }

    @Test
    void differentTurnOrForcedCaptureShouldProduceDifferentPositions() {
        GameRoom room = mock(GameRoom.class);
        when(room.getTimeControlMinutes()).thenReturn(10);
        CheckersGameState state = CheckersGameState.start(room);

        ReflectionTestUtils.setField(state, "currentTurn", PieceColor.BLACK);
        assertThat(state.recordCurrentPosition()).isEqualTo(1);

        ReflectionTestUtils.setField(state, "forcedCaptureRow", 3);
        ReflectionTestUtils.setField(state, "forcedCaptureColumn", 2);
        assertThat(state.recordCurrentPosition()).isEqualTo(1);
    }

    @Test
    void kingOnlyCounterShouldIncrementAndResetOnCaptureOrManMove() {
        GameRoom room = mock(GameRoom.class);
        when(room.getTimeControlMinutes()).thenReturn(10);
        CheckersGameState state = CheckersGameState.start(room);
        CheckersMoveResult quietMove = new CheckersMoveResult(
                CheckersBoard.initial(), false, false, new BoardPosition(4, 1));

        state.updateAutomaticDrawCounters(quietMove, true);
        state.updateAutomaticDrawCounters(quietMove, true);
        assertThat(state.getKingOnlyMoveCount()).isEqualTo(2);

        state.updateAutomaticDrawCounters(quietMove, false);
        assertThat(state.getKingOnlyMoveCount()).isZero();

        state.updateAutomaticDrawCounters(quietMove, true);
        state.updateAutomaticDrawCounters(
                new CheckersMoveResult(CheckersBoard.initial(), true, false, new BoardPosition(3, 2)),
                true);
        assertThat(state.getKingOnlyMoveCount()).isZero();
    }
}
