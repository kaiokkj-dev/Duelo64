package com.duelo64.backend.game.matchmaking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.security.SecureRandom;
import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.duelo64.backend.game.room.*;
import com.duelo64.backend.game.stats.PlayerGameRating;
import com.duelo64.backend.game.stats.PlayerGameRatingRepository;
import com.duelo64.backend.user.User;
import com.duelo64.backend.user.UserRepository;

class MatchmakingServiceTest {
    private GameRoomService roomService;
    private GameRoomRepository roomRepository;
    private UserRepository userRepository;
    private PlayerGameRatingRepository ratingRepository;
    private MatchmakingRealtimePublisher publisher;
    private MutableClock clock;
    private MatchmakingService service;
    private User first;
    private User second;

    @BeforeEach
    void setUp() {
        roomService = mock(GameRoomService.class);
        roomRepository = mock(GameRoomRepository.class);
        userRepository = mock(UserRepository.class);
        ratingRepository = mock(PlayerGameRatingRepository.class);
        publisher = mock(MatchmakingRealtimePublisher.class);
        SecureRandom random = mock(SecureRandom.class);
        when(random.nextBoolean()).thenReturn(true);
        clock = new MutableClock(Instant.parse("2026-08-10T18:00:00Z"));
        service = new MatchmakingService(
                roomService, roomRepository, userRepository, ratingRepository, publisher, random, clock);
        first = user("first");
        second = user("second");
        when(userRepository.findById(first.getId())).thenReturn(Optional.of(first));
        when(userRepository.findById(second.getId())).thenReturn(Optional.of(second));
        when(roomRepository.findActiveRoomsForUser(any(UUID.class), eq(RoomStatus.IN_PROGRESS)))
                .thenReturn(List.of());
        when(ratingRepository.findByUserIdAndGameType(any(UUID.class), eq(GameType.CHECKERS)))
                .thenReturn(Optional.empty());
        when(roomService.createRankedRoom(any(UUID.class), any(UUID.class), any(GameType.class), anyInt()))
                .thenAnswer(invocation -> rankedRoom(
                        invocation.getArgument(0), invocation.getArgument(1),
                        invocation.getArgument(2), invocation.getArgument(3)));
    }

    @Test
    void repeatedEntryIsIdempotentAndCancellationRemovesPlayer() {
        MatchmakingStatusResponse firstEntry = service.enqueue(first.getId(), 10);
        MatchmakingStatusResponse repeated = service.enqueue(first.getId(), 5);

        assertThat(firstEntry.status()).isEqualTo("QUEUED");
        assertThat(repeated.status()).isEqualTo("QUEUED");
        assertThat(repeated.timeControlMinutes()).isEqualTo(10);
        assertThat(service.cancel(first.getId()).status()).isEqualTo("IDLE");
        assertThat(service.cancel(first.getId()).status()).isEqualTo("IDLE");
    }

    @Test
    void compatiblePlayersCreateOneRankedRoomAndBothReceivePrivateEvent() {
        service.enqueue(first.getId(), 10);
        MatchmakingStatusResponse result = service.enqueue(second.getId(), 10);

        assertThat(result.status()).isEqualTo("MATCH_FOUND");
        assertThat(result.match().color()).isEqualTo("WHITE");
        verify(roomService, times(1)).createRankedRoom(second.getId(), first.getId(), GameType.CHECKERS, 10);
        verify(publisher).matchFound(eq(first.getId()), any(MatchFoundResponse.class));
        verify(publisher).matchFound(eq(second.getId()), any(MatchFoundResponse.class));
    }

    @Test
    void concurrentEntriesCannotCreateTwoRooms() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            var firstCall = executor.submit(() -> {
                start.await();
                return service.enqueue(first.getId(), 10);
            });
            var secondCall = executor.submit(() -> {
                start.await();
                return service.enqueue(second.getId(), 10);
            });
            start.countDown();
            firstCall.get();
            secondCall.get();

            verify(roomService, times(1))
                    .createRankedRoom(any(UUID.class), any(UUID.class), eq(GameType.CHECKERS), eq(10));
            verify(publisher, times(2)).matchFound(any(UUID.class), any(MatchFoundResponse.class));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void differentTimeControlsDoNotMatch() {
        service.enqueue(first.getId(), 5);
        MatchmakingStatusResponse secondStatus = service.enqueue(second.getId(), 10);

        assertThat(secondStatus.status()).isEqualTo("QUEUED");
        verifyNoInteractions(roomService);
    }

    @Test
    void differentGameTypesDoNotMatch() {
        service.enqueue(first.getId(), GameType.CHECKERS, 10);
        MatchmakingStatusResponse secondStatus = service.enqueue(second.getId(), GameType.CHESS, 10);

        assertThat(secondStatus.status()).isEqualTo("QUEUED");
        verifyNoInteractions(roomService);
    }

    @Test
    void gameTypeSelectsIndependentRatingAndRoomLifecycle() {
        service.enqueue(first.getId(), GameType.CHESS, 10);
        MatchmakingStatusResponse result = service.enqueue(second.getId(), GameType.CHESS, 10);

        assertThat(result.status()).isEqualTo("MATCH_FOUND");
        verify(ratingRepository).findByUserIdAndGameType(first.getId(), GameType.CHESS);
        verify(ratingRepository).findByUserIdAndGameType(second.getId(), GameType.CHESS);
        verify(roomService).createRankedRoom(
                any(UUID.class), any(UUID.class), eq(GameType.CHESS), eq(10));
    }

    @Test
    void playerCannotQueueForTwoGameTypesAtOnce() {
        service.enqueue(first.getId(), GameType.CHECKERS, 10);

        assertThatThrownBy(() -> service.enqueue(first.getId(), GameType.CHESS, 10))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("outra modalidade");
        verifyNoInteractions(roomService);
    }

    @Test
    void ratingComesFromBackendAndRangeExpandsWithWaitingTime() {
        PlayerGameRating firstRating = new PlayerGameRating(first, GameType.CHECKERS);
        firstRating.updateRating(1000);
        PlayerGameRating secondRating = new PlayerGameRating(second, GameType.CHECKERS);
        secondRating.updateRating(1250);
        when(ratingRepository.findByUserIdAndGameType(first.getId(), GameType.CHECKERS))
                .thenReturn(Optional.of(firstRating));
        when(ratingRepository.findByUserIdAndGameType(second.getId(), GameType.CHECKERS))
                .thenReturn(Optional.of(secondRating));

        service.enqueue(first.getId(), 10);
        assertThat(service.enqueue(second.getId(), 10).status()).isEqualTo("QUEUED");
        clock.advance(Duration.ofSeconds(61));
        service.matchWaitingPlayers();

        verify(roomService, times(1))
                .createRankedRoom(any(UUID.class), any(UUID.class), eq(GameType.CHECKERS), eq(10));
        verify(publisher, times(2)).matchFound(any(UUID.class), any(MatchFoundResponse.class));
    }

    @Test
    void activePlayerCannotEnterQueue() {
        GameRoom active = GameRoom.privateCheckers("ACTIVE", 10, first);
        active.join(second);
        when(roomRepository.findActiveRoomsForUser(first.getId(), RoomStatus.IN_PROGRESS))
                .thenReturn(List.of(active));

        assertThatThrownBy(() -> service.enqueue(first.getId(), 10))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("partida em andamento");
    }

    private GameRoom rankedRoom(UUID whiteId, UUID blackId, GameType gameType, int time) {
        User white = whiteId.equals(first.getId()) ? first : second;
        User black = blackId.equals(first.getId()) ? first : second;
        GameRoom room = GameRoom.rankedRoom("RANK01", gameType, time, white);
        ReflectionTestUtils.setField(room, "id", UUID.randomUUID());
        room.join(black);
        return room;
    }

    private User user(String nickname) {
        User user = new User(nickname + "@duelo64.com");
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        user.updateNickname(nickname);
        return user;
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        MutableClock(Instant instant) { this.instant = instant; }
        void advance(Duration duration) { instant = instant.plus(duration); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
