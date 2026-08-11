package com.duelo64.backend.game.checkers.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.util.ReflectionTestUtils;

import com.duelo64.backend.game.checkers.api.MovePieceRequest;
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
import com.duelo64.backend.game.room.RoomStatus;
import com.duelo64.backend.game.match.MatchHistoryService;
import com.duelo64.backend.user.User;

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

    @Test
    void shouldDeclareBlackVictoryWhenWhiteHasNoPieces() {
        CheckersGameState state = stateWithBoard(
                "........", "......b.", "........", "........",
                "........", "........", "........", "........");

        service().finishIfOpponentCannotPlay(state, PieceColor.BLACK);

        assertThat(state.getWinnerColor()).isEqualTo(PieceColor.BLACK);
        assertThat(state.getFinishReason()).isEqualTo(CheckersFinishReason.NO_PIECES);
    }

    @Test
    void shouldRejectMoveAfterGameIsFinished() {
        UUID playerId = UUID.randomUUID();
        GameRoom room = mock(GameRoom.class);
        CheckersGameStateRepository stateRepository = mock(CheckersGameStateRepository.class);
        CheckersGameState state = mock(CheckersGameState.class);
        when(state.getRoom()).thenReturn(room);
        when(room.getStatus()).thenReturn(RoomStatus.FINISHED);
        when(stateRepository.findByRoomCode("ABC123")).thenReturn(Optional.of(state));
        CheckersGameService service = new CheckersGameService(
                stateRepository,
                mock(GameRoomRepository.class),
                mock(RoomRealtimePublisher.class),
                mock(MatchHistoryService.class));

        assertThatThrownBy(() -> service.movePiece(
                playerId,
                "ABC123",
                new MovePieceRequest(5, 0, 4, 1)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void hostResignationShouldMakeGuestTheWinner() {
        MatchActionFixture fixture = matchActionFixture();

        fixture.service().resign(fixture.hostId(), "ABC123");

        assertThat(fixture.state().getWinnerColor()).isEqualTo(PieceColor.BLACK);
        assertThat(fixture.state().getFinishReason()).isEqualTo(CheckersFinishReason.RESIGNATION);
        assertThat(fixture.room().getStatus()).isEqualTo(RoomStatus.FINISHED);
        verify(fixture.publisher()).publish(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void guestResignationShouldMakeHostTheWinner() {
        MatchActionFixture fixture = matchActionFixture();

        fixture.service().resign(fixture.guestId(), "ABC123");

        assertThat(fixture.state().getWinnerColor()).isEqualTo(PieceColor.WHITE);
        assertThat(fixture.state().getFinishReason()).isEqualTo(CheckersFinishReason.RESIGNATION);
    }

    @Test
    void outsiderShouldNotBeAllowedToResign() {
        MatchActionFixture fixture = matchActionFixture();

        assertStatus(
                () -> fixture.service().resign(UUID.randomUUID(), "ABC123"),
                HttpStatus.FORBIDDEN);
    }

    @Test
    void resignationAfterFinishedShouldBeRejected() {
        MatchActionFixture fixture = matchActionFixture();
        fixture.room().finish();

        assertStatus(
                () -> fixture.service().resign(fixture.hostId(), "ABC123"),
                HttpStatus.CONFLICT);
    }

    @Test
    void playerShouldBeAbleToOfferDraw() {
        MatchActionFixture fixture = matchActionFixture();

        fixture.service().offerDraw(fixture.hostId(), "ABC123");

        assertThat(fixture.state().hasPendingDrawOffer()).isTrue();
        assertThat(fixture.state().getDrawOfferedByColor()).isEqualTo(PieceColor.WHITE);
    }

    @Test
    void outsiderShouldNotBeAllowedToOfferDraw() {
        MatchActionFixture fixture = matchActionFixture();

        assertStatus(
                () -> fixture.service().offerDraw(UUID.randomUUID(), "ABC123"),
                HttpStatus.FORBIDDEN);
    }

    @Test
    void playerShouldNotAcceptOwnDrawOffer() {
        MatchActionFixture fixture = matchActionFixture();
        fixture.service().offerDraw(fixture.hostId(), "ABC123");

        assertStatus(
                () -> fixture.service().acceptDraw(fixture.hostId(), "ABC123"),
                HttpStatus.FORBIDDEN);
    }

    @Test
    void opponentShouldAcceptDrawAndFinishMatch() {
        MatchActionFixture fixture = matchActionFixture();
        fixture.service().offerDraw(fixture.hostId(), "ABC123");

        fixture.service().acceptDraw(fixture.guestId(), "ABC123");

        assertThat(fixture.room().getStatus()).isEqualTo(RoomStatus.FINISHED);
        assertThat(fixture.state().getWinnerColor()).isNull();
        assertThat(fixture.state().getFinishReason()).isEqualTo(CheckersFinishReason.DRAW_AGREEMENT);
        assertThat(fixture.state().hasPendingDrawOffer()).isFalse();
    }

    @Test
    void opponentShouldDeclineDrawAndKeepMatchRunning() {
        MatchActionFixture fixture = matchActionFixture();
        fixture.service().offerDraw(fixture.hostId(), "ABC123");

        fixture.service().declineDraw(fixture.guestId(), "ABC123");

        assertThat(fixture.room().getStatus()).isEqualTo(RoomStatus.IN_PROGRESS);
        assertThat(fixture.state().hasPendingDrawOffer()).isFalse();
    }

    @Test
    void secondPendingDrawOfferShouldBeRejected() {
        MatchActionFixture fixture = matchActionFixture();
        fixture.service().offerDraw(fixture.hostId(), "ABC123");

        assertStatus(
                () -> fixture.service().offerDraw(fixture.guestId(), "ABC123"),
                HttpStatus.CONFLICT);
    }

    @Test
    void validMoveShouldCancelPendingDrawOffer() {
        MatchActionFixture fixture = matchActionFixture();
        fixture.service().offerDraw(fixture.guestId(), "ABC123");

        fixture.service().movePiece(
                fixture.hostId(),
                "ABC123",
                new MovePieceRequest(5, 0, 4, 1));

        assertThat(fixture.state().hasPendingDrawOffer()).isFalse();
        assertThat(fixture.room().getStatus()).isEqualTo(RoomStatus.IN_PROGRESS);
    }

    @Test
    void confirmedTimeoutShouldGiveVictoryToOpponent() {
        MatchActionFixture fixture = matchActionFixture(Instant.now().minusSeconds(601));

        fixture.service().confirmTimeout(fixture.hostId(), "ABC123");

        assertThat(fixture.room().getStatus()).isEqualTo(RoomStatus.FINISHED);
        assertThat(fixture.state().getWinnerColor()).isEqualTo(PieceColor.BLACK);
        assertThat(fixture.state().getFinishReason()).isEqualTo(CheckersFinishReason.TIMEOUT);
    }

    @Test
    void frontendShouldNotBeAbleToFakeTimeout() {
        MatchActionFixture fixture = matchActionFixture();

        fixture.service().confirmTimeout(fixture.hostId(), "ABC123");

        assertThat(fixture.room().getStatus()).isEqualTo(RoomStatus.IN_PROGRESS);
        assertThat(fixture.state().getFinishReason()).isNull();
    }

    @Test
    void outsiderShouldNotReadPrivateGameState() {
        MatchActionFixture fixture = matchActionFixture();

        assertStatus(
                () -> fixture.service().getState(UUID.randomUUID(), "ABC123"),
                HttpStatus.FORBIDDEN);
    }

    @Test
    void participantShouldRecoverForcedCaptureAfterReload() {
        MatchActionFixture fixture = matchActionFixture();
        fixture.state().apply(new CheckersMoveResult(
                CheckersBoard.initial(), true, true, new BoardPosition(3, 2)), Instant.now());

        CheckersGameState recovered = fixture.service().getState(fixture.guestId(), "ABC123");

        assertThat(recovered).isSameAs(fixture.state());
        assertThat(recovered.getCurrentTurn()).isEqualTo(PieceColor.WHITE);
        assertThat(recovered.mustContinueCapture()).isTrue();
    }

    @Test
    void participantShouldRecoverPendingDrawOfferAfterReload() {
        MatchActionFixture fixture = matchActionFixture();
        fixture.service().offerDraw(fixture.hostId(), "ABC123");

        CheckersGameState recovered = fixture.service().getState(fixture.guestId(), "ABC123");

        assertThat(recovered.hasPendingDrawOffer()).isTrue();
        assertThat(recovered.getDrawOfferedByColor()).isEqualTo(PieceColor.WHITE);
    }

    @Test
    void expiredMoveShouldFinishByTimeoutWithoutChangingBoard() {
        MatchActionFixture fixture = matchActionFixture(Instant.now().minusSeconds(601));
        String boardBefore = fixture.state().getBoardNotation();

        fixture.service().movePiece(
                fixture.hostId(),
                "ABC123",
                new MovePieceRequest(5, 0, 4, 1));

        assertThat(fixture.state().getMoveCount()).isZero();
        assertThat(fixture.state().getBoardNotation()).isEqualTo(boardBefore);
        assertThat(fixture.state().getFinishReason()).isEqualTo(CheckersFinishReason.TIMEOUT);
    }

    @Test
    void legalMovesShouldBeEmptyForPlayerOutsideCurrentTurn() {
        LegalMovesFixture fixture = legalMovesFixture();

        assertThat(fixture.service().legalMoves(
                fixture.guestId(), "ABC123", 2, 1)).isEmpty();
    }

    @Test
    void legalMovesShouldBeEmptyForOpponentPiece() {
        LegalMovesFixture fixture = legalMovesFixture();

        assertThat(fixture.service().legalMoves(
                fixture.hostId(), "ABC123", 2, 1)).isEmpty();
    }

    @Test
    void repetitionBelowLimitShouldNotFinishMatch() {
        MatchActionFixture fixture = matchActionFixture();

        fixture.service().finishIfAutomaticDraw(fixture.state());

        assertThat(fixture.room().getStatus()).isEqualTo(RoomStatus.IN_PROGRESS);
        assertThat(fixture.state().getFinishReason()).isNull();
    }

    @Test
    void thirdPositionOccurrenceShouldFinishAsDraw() {
        MatchActionFixture fixture = matchActionFixture();
        fixture.state().recordCurrentPosition();

        fixture.service().finishIfAutomaticDraw(fixture.state());

        assertThat(fixture.room().getStatus()).isEqualTo(RoomStatus.FINISHED);
        assertThat(fixture.state().getWinnerColor()).isNull();
        assertThat(fixture.state().getFinishReason()).isEqualTo(CheckersFinishReason.DRAW_REPETITION);
    }

    @Test
    void fortyKingOnlyMovesShouldFinishAsDraw() {
        MatchActionFixture fixture = matchActionFixture();
        ReflectionTestUtils.setField(fixture.state(), "kingOnlyMoveCount", 40);

        fixture.service().finishIfAutomaticDraw(fixture.state());

        assertThat(fixture.room().getStatus()).isEqualTo(RoomStatus.FINISHED);
        assertThat(fixture.state().getWinnerColor()).isNull();
        assertThat(fixture.state().getFinishReason()).isEqualTo(CheckersFinishReason.DRAW_MOVE_LIMIT);
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
                mock(RoomRealtimePublisher.class),
                mock(MatchHistoryService.class));
    }

    private LegalMovesFixture legalMovesFixture() {
        UUID hostId = UUID.randomUUID();
        UUID guestId = UUID.randomUUID();
        User host = mock(User.class);
        User guest = mock(User.class);
        GameRoom room = mock(GameRoom.class);
        CheckersGameStateRepository stateRepository = mock(CheckersGameStateRepository.class);

        when(host.getId()).thenReturn(hostId);
        when(guest.getId()).thenReturn(guestId);
        when(room.getHost()).thenReturn(host);
        when(room.getGuest()).thenReturn(guest);
        when(room.getStatus()).thenReturn(RoomStatus.IN_PROGRESS);
        when(room.getTimeControlMinutes()).thenReturn(10);

        CheckersGameState state = CheckersGameState.start(room);
        state.startClock(Instant.now());
        when(stateRepository.findByRoomCode("ABC123")).thenReturn(Optional.of(state));

        CheckersGameService service = new CheckersGameService(
                stateRepository,
                mock(GameRoomRepository.class),
                mock(RoomRealtimePublisher.class),
                mock(MatchHistoryService.class));

        return new LegalMovesFixture(service, hostId, guestId);
    }

    private MatchActionFixture matchActionFixture() {
        return matchActionFixture(Instant.now());
    }

    private MatchActionFixture matchActionFixture(Instant startedAt) {
        UUID hostId = UUID.randomUUID();
        UUID guestId = UUID.randomUUID();
        User host = mock(User.class);
        User guest = mock(User.class);
        when(host.getId()).thenReturn(hostId);
        when(guest.getId()).thenReturn(guestId);

        GameRoom room = GameRoom.privateCheckers("ABC123", 10, host);
        room.join(guest);
        CheckersGameState state = CheckersGameState.start(room);
        state.startClock(startedAt);
        CheckersGameStateRepository stateRepository = mock(CheckersGameStateRepository.class);
        RoomRealtimePublisher publisher = mock(RoomRealtimePublisher.class);
        when(stateRepository.findByRoomCode("ABC123")).thenReturn(Optional.of(state));

        CheckersGameService service = new CheckersGameService(
                stateRepository,
                mock(GameRoomRepository.class),
                publisher,
                mock(MatchHistoryService.class));
        return new MatchActionFixture(service, state, room, publisher, hostId, guestId);
    }

    private void assertStatus(org.assertj.core.api.ThrowableAssert.ThrowingCallable action, HttpStatus status) {
        assertThatThrownBy(action)
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(status));
    }

    private record LegalMovesFixture(
            CheckersGameService service,
            UUID hostId,
            UUID guestId) {
    }

    private record MatchActionFixture(
            CheckersGameService service,
            CheckersGameState state,
            GameRoom room,
            RoomRealtimePublisher publisher,
            UUID hostId,
            UUID guestId) {
    }
}
