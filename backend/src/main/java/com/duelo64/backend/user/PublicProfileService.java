package com.duelo64.backend.user;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.duelo64.backend.game.match.MatchHistoryService;
import com.duelo64.backend.game.room.GameType;
import com.duelo64.backend.game.stats.PlayerStatsService;

@Service
public class PublicProfileService {
    private final UserRepository userRepository;
    private final PlayerStatsService playerStatsService;
    private final MatchHistoryService matchHistoryService;

    public PublicProfileService(
            UserRepository userRepository,
            PlayerStatsService playerStatsService,
            MatchHistoryService matchHistoryService) {
        this.userRepository = userRepository;
        this.playerStatsService = playerStatsService;
        this.matchHistoryService = matchHistoryService;
    }

    @Transactional(readOnly = true)
    public PublicProfileResponse findById(UUID userId) {
        return findById(userId, GameType.CHECKERS);
    }

    @Transactional(readOnly = true)
    public PublicProfileResponse findById(UUID userId, GameType gameType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Jogador nao encontrado."));
        var stats = playerStatsService.statsFor(userId, gameType);
        var matches = matchHistoryService.historyFor(userId, gameType).stream()
                .map(match -> PublicMatchHistoryItemResponse.from(match, userId))
                .toList();
        return PublicProfileResponse.from(user, stats, matches);
    }
}
