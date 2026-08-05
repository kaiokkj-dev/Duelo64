package com.duelo64.backend.game.checkers.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.duelo64.backend.game.checkers.domain.BoardPosition;
import com.duelo64.backend.game.checkers.domain.CheckersBoard;
import com.duelo64.backend.game.checkers.domain.CheckersMoveResult;
import com.duelo64.backend.game.checkers.domain.PieceColor;
import com.duelo64.backend.game.checkers.persistence.CheckersFinishReason;
import com.duelo64.backend.game.checkers.persistence.CheckersGameState;
import com.duelo64.backend.game.checkers.persistence.CheckersGameStateRepository;
import com.duelo64.backend.game.room.GameRoom;
import com.duelo64.backend.game.room.GameRoomRepository;
import com.duelo64.backend.game.room.RoomRealtimePublisher;

class CheckersGameServiceTest {

    @Test
    void shouldDeclareVictoryWhenOpponentHasNoPieces() {
        CheckersGameState state = stateWithBoard(
                "........", "........", "........", "........",
                "........", "........", ".w......", "........");
        CheckersGameService service = service();

        service.finishIfOpponentCannotPlay(state, PieceColor.WHITE);

        assertThat(state.getWinnerColor()).isEqualTo(PieceColor.WHITE);
        assertThat(state.getFinishReason()).isEqualTo(CheckersFinishReason.NO_PIECES);
    }

    @Test
    void shouldDeclareVictoryWhenOpponentHasNoLegalMoves() {
        CheckersGameState state = stateWithBoard(
                ".w......", "........", "........", "........",
                "........", "........", "........", "b.......");
        CheckersGameService service = service();

        service.finishIfOpponentCannotPlay(state, PieceColor.WHITE);

        assertThat(state.getWinnerColor()).isEqualTo(PieceColor.WHITE);
        assertThat(state.getFinishReason()).isEqualTo(CheckersFinishReason.NO_LEGAL_MOVES);
    }

    private CheckersGameState stateWithBoard(String... rows) {
        GameRoom room = mock(GameRoom.class);
        when(room.getTimeControlMinutes()).thenReturn(10);
        CheckersGameState state = CheckersGameState.start(room);
        CheckersBoard board = CheckersBoard.fromNotation(String.join("", rows));
        state.apply(new CheckersMoveResult(board, false, true, new BoardPosition(0, 0)), Instant.now());
        return state;
    }

    private CheckersGameService service() {
        return new CheckersGameService(
                mock(CheckersGameStateRepository.class),
                mock(GameRoomRepository.class),
                mock(RoomRealtimePublisher.class));
    }
}
