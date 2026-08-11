package com.duelo64.backend.game.chess.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.duelo64.backend.game.chess.domain.*;
import com.duelo64.backend.game.chess.persistence.*;
import com.duelo64.backend.game.room.*;
import com.duelo64.backend.user.User;

class ChessGameTerminationTest {

    @Test
    void checkmateFinishesWithWinnerAndLoser() {
        Fixture fixture = fixture("7k/6Q1/5K2/8/8/8/8/8 b - - 0 1");

        fixture.service.finishIfRequired(fixture.state);

        assertThat(fixture.state.getFinishReason()).isEqualTo(ChessFinishReason.CHECKMATE);
        assertThat(fixture.state.getWinnerColor()).isEqualTo(ChessColor.WHITE);
        assertThat(fixture.state.getLoserColor()).isEqualTo(ChessColor.BLACK);
        assertThat(fixture.state.getFinishedAt()).isNotNull();
        assertThat(fixture.room.getStatus()).isEqualTo(RoomStatus.FINISHED);
    }

    @Test
    void checkWithAvailableDefenseIsNotMate() {
        Fixture fixture = fixture("4kb2/8/8/8/8/8/8/4R1K1 b - - 0 1");

        fixture.service.finishIfRequired(fixture.state);

        assertThat(fixture.state.getFinishReason()).isNull();
        assertThat(fixture.room.getStatus()).isEqualTo(RoomStatus.IN_PROGRESS);
    }

    @Test
    void stalemateFinishesAsDraw() {
        Fixture fixture = fixture("7k/5K2/6Q1/8/8/8/8/8 b - - 0 1");

        fixture.service.finishIfRequired(fixture.state);

        assertDraw(fixture, ChessFinishReason.STALEMATE);
    }

    @Test
    void fiftyMoveRuleFinishesAtOneHundredHalfmoves() {
        Fixture fixture = fixture("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 100 51");

        fixture.service.finishIfRequired(fixture.state);

        assertDraw(fixture, ChessFinishReason.DRAW_FIFTY_MOVE_RULE);
    }

    @Test
    void thirdOccurrenceFinishesAndCastlingRightsDistinguishPositions() {
        Fixture fixture = fixture("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 4 8");
        ReflectionTestUtils.setField(fixture.state, "positionOccurrences", "");
        assertThat(fixture.state.recordCurrentPosition(false)).isOne();
        assertThat(fixture.state.recordCurrentPosition(false)).isEqualTo(2);

        fixture.service.finishIfRequired(fixture.state);

        assertDraw(fixture, ChessFinishReason.DRAW_REPETITION);

        Fixture distinct = fixture("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 4 8");
        ReflectionTestUtils.setField(distinct.state, "positionOccurrences", "");
        assertThat(distinct.state.recordCurrentPosition(false)).isOne();
        ReflectionTestUtils.setField(distinct.state, "fen", "r3k2r/8/8/8/8/8/8/R3K2R w - - 4 8");
        assertThat(distinct.state.recordCurrentPosition(false)).isOne();
    }

    @Test
    void insufficientMaterialFinishesButSufficientMaterialContinues() {
        Fixture insufficient = fixture("4k3/8/8/8/8/8/8/4K3 w - - 0 1");
        insufficient.service.finishIfRequired(insufficient.state);
        assertDraw(insufficient, ChessFinishReason.DRAW_INSUFFICIENT_MATERIAL);

        Fixture sufficient = fixture("4k3/8/8/8/8/8/8/R3K3 w - - 0 1");
        sufficient.service.finishIfRequired(sufficient.state);
        assertThat(sufficient.state.getFinishReason()).isNull();
    }

    @Test
    void halfmoveClockIncrementsAndPawnMoveResetsIt() {
        Fixture fixture = fixture("4k3/4p3/8/8/8/8/8/4K1N1 w - - 7 1");
        ChessRules rules = new ChessRules();
        fixture.state.apply(rules.applyMove(fixture.state.chessFen(), p(7, 6), p(5, 5), null));
        assertThat(fixture.state.getHalfmoveClock()).isEqualTo(8);
        fixture.state.apply(rules.applyMove(fixture.state.chessFen(), p(1, 4), p(3, 4), null));
        assertThat(fixture.state.getHalfmoveClock()).isZero();
    }

    @Test
    void moveAfterFinishedIsRejectedWithoutPersistence() {
        Fixture fixture = fixture(ChessFen.initial().notation());
        fixture.room.finish();
        ChessGameStateRepository repository = mock(ChessGameStateRepository.class);
        when(repository.findByRoomCode("CHSTEST")).thenReturn(Optional.of(fixture.state));
        ChessGameService service = new ChessGameService(repository, mock(GameRoomRepository.class), mock(RoomRealtimePublisher.class), mock(com.duelo64.backend.game.match.MatchHistoryService.class));

        assertThatThrownBy(() -> service.move(fixture.whiteId, "CHSTEST", p(6, 4), p(4, 4)))
                .isInstanceOf(ResponseStatusException.class);
        verify(repository, never()).save(any());
    }

    private void assertDraw(Fixture fixture, ChessFinishReason reason) {
        assertThat(fixture.state.getFinishReason()).isEqualTo(reason);
        assertThat(fixture.state.getWinnerColor()).isNull();
        assertThat(fixture.state.getLoserColor()).isNull();
        assertThat(fixture.room.getStatus()).isEqualTo(RoomStatus.FINISHED);
    }

    private Fixture fixture(String fen) {
        UUID whiteId = UUID.randomUUID();
        User white = user(whiteId);
        User black = user(UUID.randomUUID());
        GameRoom room = GameRoom.rankedRoom("CHSTEST", GameType.CHESS, 10, white);
        room.join(black);
        ChessGameState state = ChessGameState.start(room);
        ReflectionTestUtils.setField(state, "fen", fen);
        return new Fixture(whiteId, room, state,
                new ChessGameService(mock(ChessGameStateRepository.class), mock(GameRoomRepository.class), mock(RoomRealtimePublisher.class), mock(com.duelo64.backend.game.match.MatchHistoryService.class)));
    }

    private User user(UUID id) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        return user;
    }

    private ChessPosition p(int row, int column) { return new ChessPosition(row, column); }
    private record Fixture(UUID whiteId, GameRoom room, ChessGameState state, ChessGameService service) {}
}

