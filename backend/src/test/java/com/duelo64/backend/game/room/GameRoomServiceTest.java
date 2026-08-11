package com.duelo64.backend.game.room;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.duelo64.backend.game.checkers.persistence.CheckersGameState;
import com.duelo64.backend.user.User;
import com.duelo64.backend.user.UserRepository;

class GameRoomServiceTest {

    @Test
    void rankedRoomStartsImmediatelyWithCleanStateAndClock() {
        Fixture fixture = fixture(false);

        GameRoom ranked = fixture.service().createRankedCheckersRoom(
                fixture.hostId(), fixture.guestId(), 10);

        assertThat(ranked.getMatchType()).isEqualTo(MatchType.RANKED);
        assertThat(ranked.getRoomType()).isEqualTo(RoomType.PUBLIC);
        assertThat(ranked.getStatus()).isEqualTo(RoomStatus.IN_PROGRESS);
        assertThat(ranked.getHost().getId()).isEqualTo(fixture.hostId());
        assertThat(ranked.getGuest().getId()).isEqualTo(fixture.guestId());
        assertThat(fixture.newState().get().getTurnStartedAt()).isNotNull();
    }

    @Test
    void hostRejoiningShouldKeepHostAndWhiteSeat() {
        Fixture fixture = fixture(true);

        GameRoom returned = fixture.service().joinRoom(fixture.hostId(), "ABC123");

        assertThat(returned).isSameAs(fixture.room());
        assertThat(returned.getHost().getId()).isEqualTo(fixture.hostId());
        assertThat(returned.getGuest().getId()).isEqualTo(fixture.guestId());
    }

    @Test
    void guestRejoiningShouldKeepGuestAndBlackSeat() {
        Fixture fixture = fixture(true);

        GameRoom returned = fixture.service().joinRoom(fixture.guestId(), "ABC123");

        assertThat(returned).isSameAs(fixture.room());
        assertThat(returned.getGuest().getId()).isEqualTo(fixture.guestId());
    }

    @Test
    void thirdUserShouldNotJoinFullRoom() {
        Fixture fixture = fixture(true);
        UUID outsiderId = UUID.randomUUID();
        User outsider = user(outsiderId);
        when(fixture.userRepository().findById(outsiderId)).thenReturn(Optional.of(outsider));

        assertThatThrownBy(() -> fixture.service().joinRoom(outsiderId, "ABC123"))
                .isInstanceOf(RoomUnavailableException.class);
    }

    @Test
    void participantShouldReopenFinishedRoom() {
        Fixture fixture = fixture(true);
        fixture.room().finish();

        GameRoom returned = fixture.service().joinRoom(fixture.guestId(), "ABC123");

        assertThat(returned).isSameAs(fixture.room());
        assertThat(returned.getStatus()).isEqualTo(RoomStatus.FINISHED);
        assertThat(returned.getGuest().getId()).isEqualTo(fixture.guestId());
    }

    @Test
    void outsiderShouldNotReadPrivateRoom() {
        Fixture fixture = fixture(true);

        assertThatThrownBy(() -> fixture.service().getRoom(UUID.randomUUID(), "ABC123"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void rematchCanOnlyBeRequestedAfterFinished() {
        Fixture fixture = fixture(true);

        assertStatus(
                () -> fixture.service().requestRematch(fixture.hostId(), "ABC123"),
                HttpStatus.CONFLICT);
    }

    @Test
    void outsiderCannotRequestRematch() {
        Fixture fixture = finishedFixture();

        assertStatus(
                () -> fixture.service().requestRematch(UUID.randomUUID(), "ABC123"),
                HttpStatus.FORBIDDEN);
    }

    @Test
    void requesterCannotAcceptOwnRematch() {
        Fixture fixture = finishedFixture();
        fixture.service().requestRematch(fixture.hostId(), "ABC123");

        assertStatus(
                () -> fixture.service().acceptRematch(fixture.hostId(), "ABC123"),
                HttpStatus.FORBIDDEN);
    }

    @Test
    void opponentCanDeclineRematch() {
        Fixture fixture = finishedFixture();
        fixture.service().requestRematch(fixture.hostId(), "ABC123");

        fixture.service().declineRematch(fixture.guestId(), "ABC123");

        assertThat(fixture.room().hasPendingRematch()).isFalse();
        assertThat(fixture.room().getStatus()).isEqualTo(RoomStatus.FINISHED);
    }

    @Test
    void acceptedRematchCreatesCleanRoomWithPlayersAndColorsInverted() {
        Fixture fixture = finishedFixture();
        fixture.service().requestRematch(fixture.hostId(), "ABC123");

        GameRoom rematch = fixture.service().acceptRematch(fixture.guestId(), "ABC123");

        assertThat(rematch).isNotSameAs(fixture.room());
        assertThat(rematch.getHost().getId()).isEqualTo(fixture.guestId());
        assertThat(rematch.getGuest().getId()).isEqualTo(fixture.hostId());
        assertThat(rematch.getTimeControlMinutes()).isEqualTo(10);
        assertThat(rematch.getStatus()).isEqualTo(RoomStatus.IN_PROGRESS);
        assertThat(fixture.room().getStatus()).isEqualTo(RoomStatus.FINISHED);
        assertThat(fixture.room().getRematchRoomCode()).isEqualTo(rematch.getCode());

        CheckersGameState newState = fixture.newState().get();
        assertThat(newState.getMoveCount()).isZero();
        assertThat(newState.getWinnerColor()).isNull();
        assertThat(newState.getFinishReason()).isNull();
        assertThat(newState.hasPendingDrawOffer()).isFalse();
        assertThat(newState.mustContinueCapture()).isFalse();
        assertThat(newState.getWhiteRemainingMillis()).isEqualTo(600_000L);
        assertThat(newState.getBlackRemainingMillis()).isEqualTo(600_000L);
        verify(fixture.publisher()).publish(org.mockito.ArgumentMatchers.argThat(
                event -> event.type().equals("REMATCH_ACCEPTED")
                        && event.newRoomCode().equals(rematch.getCode())));
    }

    @Test
    void repeatedAcceptanceReturnsSameRoomWithoutCreatingAnother() {
        Fixture fixture = finishedFixture();
        fixture.service().requestRematch(fixture.hostId(), "ABC123");
        GameRoom first = fixture.service().acceptRematch(fixture.guestId(), "ABC123");
        when(fixture.roomRepository().findByCode(first.getCode())).thenReturn(Optional.of(first));

        GameRoom second = fixture.service().acceptRematch(fixture.guestId(), "ABC123");

        assertThat(second).isSameAs(first);
        verify(fixture.roomRepository(), times(1)).save(any(GameRoom.class));
    }

    private Fixture finishedFixture() {
        Fixture fixture = fixture(true);
        fixture.room().finish();
        return fixture;
    }

    private Fixture fixture(boolean withGuest) {
        UUID hostId = UUID.randomUUID();
        UUID guestId = UUID.randomUUID();
        User host = user(hostId);
        User guest = user(guestId);
        GameRoom room = GameRoom.privateCheckers("ABC123", 10, host);
        if (withGuest) room.join(guest);

        GameRoomRepository roomRepository = mock(GameRoomRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        when(roomRepository.findByCode("ABC123")).thenReturn(Optional.of(room));
        when(roomRepository.findByCodeForUpdate("ABC123")).thenReturn(Optional.of(room));
        when(roomRepository.save(any(GameRoom.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(hostId)).thenReturn(Optional.of(host));
        when(userRepository.findById(guestId)).thenReturn(Optional.of(guest));

        AtomicReference<CheckersGameState> newState = new AtomicReference<>();
        GameStateLifecycle lifecycle = new GameStateLifecycle() {
            public GameType gameType() { return GameType.CHECKERS; }
            public void initialize(GameRoom gameRoom) {
                newState.set(CheckersGameState.start(gameRoom));
            }
            public void start(GameRoom gameRoom) {
                newState.get().startClock(gameRoom.getStartedAt());
            }
        };
        RoomRealtimePublisher publisher = mock(RoomRealtimePublisher.class);
        GameRoomService service = new GameRoomService(
                roomRepository,
                userRepository,
                java.util.List.of(lifecycle),
                publisher);
        return new Fixture(service, room, roomRepository, userRepository, publisher, newState, hostId, guestId);
    }

    private User user(UUID id) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        return user;
    }

    private void assertStatus(org.assertj.core.api.ThrowableAssert.ThrowingCallable action, HttpStatus status) {
        assertThatThrownBy(action)
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(status));
    }

    private record Fixture(
            GameRoomService service,
            GameRoom room,
            GameRoomRepository roomRepository,
            UserRepository userRepository,
            RoomRealtimePublisher publisher,
            AtomicReference<CheckersGameState> newState,
            UUID hostId,
            UUID guestId) {
    }
}
