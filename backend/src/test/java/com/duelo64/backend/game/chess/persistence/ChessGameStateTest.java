package com.duelo64.backend.game.chess.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.duelo64.backend.game.chess.domain.*;
import com.duelo64.backend.game.room.*;
import com.duelo64.backend.user.User;

class ChessGameStateTest {
    @Test
    void startsCleanAndAppliesValidMoveAtomically() {
        ChessGameState state = ChessGameState.start(chessRoom());
        ChessMoveResult move = new ChessRules().applyMove(
                ChessBoard.initial(), ChessColor.WHITE,
                new ChessPosition(6, 4), new ChessPosition(4, 4));

        state.apply(move);

        assertThat(state.chessFen().board().toFenPlacement()).isEqualTo(move.board().toFenPlacement());
        assertThat(state.getCurrentTurn()).isEqualTo(ChessColor.BLACK);
        assertThat(state.getMoveCount()).isOne();
        assertThat(state.getHalfmoveClock()).isZero();
    }

    @Test
    void refusesCheckersRoom() {
        User host = mock(User.class);
        when(host.getId()).thenReturn(UUID.randomUUID());
        GameRoom room = GameRoom.privateRoom("CHK001", GameType.CHECKERS, 10, host);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> ChessGameState.start(room))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private GameRoom chessRoom() {
        User host = mock(User.class);
        when(host.getId()).thenReturn(UUID.randomUUID());
        return GameRoom.privateRoom("CHS001", GameType.CHESS, 10, host);
    }
}

