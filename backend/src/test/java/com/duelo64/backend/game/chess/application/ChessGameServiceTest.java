package com.duelo64.backend.game.chess.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.duelo64.backend.game.chess.domain.*;
import com.duelo64.backend.game.chess.persistence.*;
import com.duelo64.backend.game.room.*;
import com.duelo64.backend.user.User;

class ChessGameServiceTest {
    @Test
    void validMoveAndLegalMovesUseOfficialEngineAndPublishRealtime() {
        UUID whiteId = UUID.randomUUID();
        GameRoom room = GameRoom.rankedRoom("CHSAPI", GameType.CHESS, 10, user(whiteId));
        room.join(user(UUID.randomUUID()));
        ChessGameState state = ChessGameState.start(room);
        state.startClock(room.getStartedAt());
        ChessGameStateRepository states = mock(ChessGameStateRepository.class);
        RoomRealtimePublisher publisher = mock(RoomRealtimePublisher.class);
        when(states.findByRoomCode("CHSAPI")).thenReturn(Optional.of(state));
        when(states.save(state)).thenReturn(state);
        ChessGameService service = new ChessGameService(states, mock(GameRoomRepository.class), publisher,
                mock(com.duelo64.backend.game.match.MatchHistoryService.class));

        assertThat(service.legalMoves(whiteId, "CHSAPI", new ChessPosition(6, 4)))
                .extracting(ChessLegalMove::toRow, ChessLegalMove::toColumn)
                .contains(org.assertj.core.groups.Tuple.tuple(5, 4), org.assertj.core.groups.Tuple.tuple(4, 4));

        ChessGameState moved = service.move(whiteId, "CHSAPI", new ChessPosition(6, 4), new ChessPosition(4, 4));

        assertThat(moved.getMoveCount()).isOne();
        assertThat(moved.getCurrentTurn()).isEqualTo(ChessColor.BLACK);
        verify(publisher).publish(argThat(event -> event.type().equals("GAME_STATE_UPDATED")));
    }

    @Test
    void drawOfferCanBeAcceptedOnlyByOpponent() {
        UUID whiteId = UUID.randomUUID();
        UUID blackId = UUID.randomUUID();
        GameRoom room = GameRoom.rankedRoom("CHSDRW", GameType.CHESS, 10, user(whiteId));
        room.join(user(blackId));
        ChessGameState state = ChessGameState.start(room);
        ChessGameStateRepository states = mock(ChessGameStateRepository.class);
        when(states.findByRoomCode("CHSDRW")).thenReturn(Optional.of(state));
        ChessGameService service = new ChessGameService(states, mock(GameRoomRepository.class), mock(RoomRealtimePublisher.class), mock(com.duelo64.backend.game.match.MatchHistoryService.class));

        service.offerDraw(whiteId, "CHSDRW");
        assertThat(state.getDrawOfferedByColor()).isEqualTo(ChessColor.WHITE);
        assertThatThrownBy(() -> service.acceptDraw(whiteId, "CHSDRW"))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        service.acceptDraw(blackId, "CHSDRW");

        assertThat(state.getFinishReason()).isEqualTo(ChessFinishReason.DRAW_AGREEMENT);
        assertThat(room.getStatus()).isEqualTo(RoomStatus.FINISHED);
    }

    @Test
    void resignationMakesOpponentWinner() {
        UUID whiteId = UUID.randomUUID();
        UUID blackId = UUID.randomUUID();
        GameRoom room = GameRoom.rankedRoom("CHSRES", GameType.CHESS, 10, user(whiteId));
        room.join(user(blackId));
        ChessGameState state = ChessGameState.start(room);
        ChessGameStateRepository states = mock(ChessGameStateRepository.class);
        when(states.findByRoomCode("CHSRES")).thenReturn(Optional.of(state));
        ChessGameService service = new ChessGameService(states, mock(GameRoomRepository.class), mock(RoomRealtimePublisher.class), mock(com.duelo64.backend.game.match.MatchHistoryService.class));

        service.resign(blackId, "CHSRES");

        assertThat(state.getWinnerColor()).isEqualTo(ChessColor.WHITE);
        assertThat(state.getFinishReason()).isEqualTo(ChessFinishReason.RESIGNATION);
    }
    @Test
    void invalidMoveDoesNotPersistChangedState() {
        UUID whiteId = UUID.randomUUID();
        User white = user(whiteId);
        User black = user(UUID.randomUUID());
        GameRoom room = GameRoom.rankedRoom("CHS001", GameType.CHESS, 10, white);
        room.join(black);
        ChessGameState state = ChessGameState.start(room);
        ChessGameStateRepository states = mock(ChessGameStateRepository.class);
        when(states.findByRoomCode("CHS001")).thenReturn(Optional.of(state));
        ChessGameService service = new ChessGameService(states, mock(GameRoomRepository.class), mock(RoomRealtimePublisher.class), mock(com.duelo64.backend.game.match.MatchHistoryService.class));

        assertThatThrownBy(() -> service.move(
                whiteId, "CHS001", new ChessPosition(1, 0), new ChessPosition(2, 0)))
                .isInstanceOf(InvalidChessMoveException.class);

        verify(states, never()).save(any(ChessGameState.class));
    }

    @Test
    void lifecycleCreatesOnlyChessState() {
        ChessGameService service = mock(ChessGameService.class);
        ChessGameStateLifecycle lifecycle = new ChessGameStateLifecycle(service, mock(ChessGameStateRepository.class));
        GameRoom room = GameRoom.privateRoom("CHS002", GameType.CHESS, 10, user(UUID.randomUUID()));

        lifecycle.initialize(room);

        verify(service).createInitialState(room);
        org.assertj.core.api.Assertions.assertThat(lifecycle.gameType()).isEqualTo(GameType.CHESS);
    }

    private User user(UUID id) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        return user;
    }
}

