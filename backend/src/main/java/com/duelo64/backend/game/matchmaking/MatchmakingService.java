package com.duelo64.backend.game.matchmaking;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.scheduling.annotation.Scheduled;

import com.duelo64.backend.game.room.*;
import com.duelo64.backend.game.stats.PlayerGameRating;
import com.duelo64.backend.game.stats.PlayerGameRatingRepository;
import com.duelo64.backend.user.User;
import com.duelo64.backend.user.UserRepository;

@Service
public class MatchmakingService {
    private final Map<UUID, MatchmakingQueueEntry> queue = new LinkedHashMap<>();
    private final Map<UUID, MatchFoundResponse> foundMatches = new LinkedHashMap<>();
    private final GameRoomService roomService;
    private final GameRoomRepository roomRepository;
    private final UserRepository userRepository;
    private final PlayerGameRatingRepository ratingRepository;
    private final MatchmakingRealtimePublisher realtimePublisher;
    private final SecureRandom random;
    private final Clock clock;

    @Autowired
    public MatchmakingService(
            GameRoomService roomService,
            GameRoomRepository roomRepository,
            UserRepository userRepository,
            PlayerGameRatingRepository ratingRepository,
            MatchmakingRealtimePublisher realtimePublisher) {
        this(roomService, roomRepository, userRepository, ratingRepository, realtimePublisher,
                new SecureRandom(), Clock.systemUTC());
    }

    MatchmakingService(
            GameRoomService roomService,
            GameRoomRepository roomRepository,
            UserRepository userRepository,
            PlayerGameRatingRepository ratingRepository,
            MatchmakingRealtimePublisher realtimePublisher,
            SecureRandom random,
            Clock clock) {
        this.roomService = roomService;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.ratingRepository = ratingRepository;
        this.realtimePublisher = realtimePublisher;
        this.random = random;
        this.clock = clock;
    }

    public synchronized MatchmakingStatusResponse enqueue(UUID userId, int timeControlMinutes) {
        return enqueue(userId, GameType.CHECKERS, timeControlMinutes);
    }

    public synchronized MatchmakingStatusResponse enqueue(
            UUID userId, GameType gameType, int timeControlMinutes) {
        Optional<MatchFoundResponse> activeRankedMatch = activeRankedMatch(userId, gameType);
        if (activeRankedMatch.isPresent()) {
            foundMatches.put(userId, activeRankedMatch.get());
            return MatchmakingStatusResponse.found(activeRankedMatch.get());
        }
        foundMatches.remove(userId);

        MatchmakingQueueEntry existing = queue.get(userId);
        if (existing != null) {
            if (existing.gameType() != gameType) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Voce ja esta na fila de outra modalidade.");
            }
            return MatchmakingStatusResponse.queued(existing);
        }
        ensureNotPlaying(userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado."));
        int rating = ratingRepository.findByUserIdAndGameType(userId, gameType)
                .map(PlayerGameRating::getRating)
                .orElse(PlayerGameRating.INITIAL_RATING);
        MatchmakingQueueEntry entry = new MatchmakingQueueEntry(
                userId, gameType, timeControlMinutes, rating, clock.instant());

        Optional<MatchmakingQueueEntry> opponent = queue.values().stream()
                .filter(candidate -> isCompatible(entry, candidate))
                .min(Comparator.comparing(MatchmakingQueueEntry::queuedAt));
        if (opponent.isEmpty()) {
            queue.put(userId, entry);
            return MatchmakingStatusResponse.queued(entry);
        }

        match(entry, opponent.get());
        return MatchmakingStatusResponse.found(foundMatches.get(userId));
    }

    @Scheduled(fixedDelayString = "${MATCHMAKING_SCAN_MILLISECONDS:5000}")
    public synchronized void matchWaitingPlayers() {
        while (true) {
            var entries = queue.values().stream()
                    .sorted(Comparator.comparing(MatchmakingQueueEntry::queuedAt))
                    .toList();
            MatchmakingQueueEntry first = null;
            MatchmakingQueueEntry second = null;
            for (int left = 0; left < entries.size() && first == null; left++) {
                for (int right = left + 1; right < entries.size(); right++) {
                    if (isCompatible(entries.get(left), entries.get(right))) {
                        first = entries.get(left);
                        second = entries.get(right);
                        break;
                    }
                }
            }
            if (first == null) return;
            match(first, second);
        }
    }

    public synchronized MatchmakingStatusResponse cancel(UUID userId) {
        return cancel(userId, GameType.CHECKERS);
    }

    public synchronized MatchmakingStatusResponse cancel(UUID userId, GameType gameType) {
        MatchmakingQueueEntry entry = queue.get(userId);
        if (entry != null && entry.gameType() == gameType) queue.remove(userId);
        return activeRankedMatch(userId, gameType)
                .map(MatchmakingStatusResponse::found)
                .orElseGet(MatchmakingStatusResponse::idle);
    }

    public synchronized MatchmakingStatusResponse status(UUID userId) {
        return status(userId, GameType.CHECKERS);
    }

    public synchronized MatchmakingStatusResponse status(UUID userId, GameType gameType) {
        Optional<MatchFoundResponse> activeMatch = activeRankedMatch(userId, gameType);
        if (activeMatch.isPresent()) {
            foundMatches.put(userId, activeMatch.get());
            return MatchmakingStatusResponse.found(activeMatch.get());
        }
        foundMatches.remove(userId);
        MatchmakingQueueEntry entry = queue.get(userId);
        if (entry != null && entry.gameType() == gameType) return MatchmakingStatusResponse.queued(entry);
        return MatchmakingStatusResponse.idle();
    }

    private boolean isCompatible(MatchmakingQueueEntry entry, MatchmakingQueueEntry candidate) {
        if (entry.userId().equals(candidate.userId())) return false;
        if (entry.gameType() != candidate.gameType()) return false;
        if (entry.timeControlMinutes() != candidate.timeControlMinutes()) return false;
        int allowedDifference = Math.max(ratingRange(entry), ratingRange(candidate));
        return Math.abs(entry.rating() - candidate.rating()) <= allowedDifference;
    }

    private void match(MatchmakingQueueEntry first, MatchmakingQueueEntry second) {
        UUID whiteId = random.nextBoolean() ? first.userId() : second.userId();
        UUID blackId = whiteId.equals(first.userId()) ? second.userId() : first.userId();
        GameRoom room = roomService.createRankedRoom(
                whiteId, blackId, first.gameType(), first.timeControlMinutes());
        queue.remove(first.userId());
        queue.remove(second.userId());

        MatchFoundResponse whiteResult = responseFor(room, whiteId, "WHITE");
        MatchFoundResponse blackResult = responseFor(room, blackId, "BLACK");
        foundMatches.put(whiteId, whiteResult);
        foundMatches.put(blackId, blackResult);
        realtimePublisher.matchFound(whiteId, whiteResult);
        realtimePublisher.matchFound(blackId, blackResult);
    }

    private int ratingRange(MatchmakingQueueEntry entry) {
        long seconds = Math.max(0, Duration.between(entry.queuedAt(), clock.instant()).toSeconds());
        if (seconds < 30) return 100;
        if (seconds < 60) return 200;
        return 300 + (int) ((seconds - 60) / 30) * 100;
    }

    private void ensureNotPlaying(UUID userId) {
        if (!roomRepository.findActiveRoomsForUser(userId, RoomStatus.IN_PROGRESS).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Voce ja esta em uma partida em andamento.");
        }
    }

    private Optional<MatchFoundResponse> activeRankedMatch(UUID userId, GameType gameType) {
        return roomRepository.findActiveRoomsForUser(userId, RoomStatus.IN_PROGRESS).stream()
                .filter(room -> room.getMatchType() == MatchType.RANKED)
                .filter(room -> gameType == null || room.getGameType() == gameType)
                .findFirst()
                .map(room -> {
                    boolean white = room.getHost().getId().equals(userId);
                    return responseFor(room, userId, white ? "WHITE" : "BLACK");
                });
    }

    private MatchFoundResponse responseFor(GameRoom room, UUID userId, String color) {
        User opponent = room.getHost().getId().equals(userId) ? room.getGuest() : room.getHost();
        return new MatchFoundResponse(
                room.getCode(), color, RoomPlayerResponse.from(opponent), room.getTimeControlMinutes());
    }
}
