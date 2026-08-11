package com.duelo64.backend.game.match;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.duelo64.backend.game.stats.EloRatingService;
import com.duelo64.backend.game.room.GameType;

@Service
public class MatchHistoryService {
    private final GameMatchRepository repository;
    private final EloRatingService eloRatingService;

    public MatchHistoryService(GameMatchRepository repository, EloRatingService eloRatingService) {
        this.repository = repository;
        this.eloRatingService = eloRatingService;
    }

    @Transactional
    public void recordCompletedMatch(CompletedMatchSnapshot snapshot) {
        if (repository.existsByRoomId(snapshot.room().getId())) return;
        GameMatch match = GameMatch.from(snapshot);
        repository.save(match);
        eloRatingService.process(match);
    }

    public List<GameMatch> historyFor(UUID userId) {
        return repository.findTop20ByWhitePlayerIdOrBlackPlayerIdOrderByFinishedAtDesc(userId, userId);
    }

    public List<GameMatch> historyFor(UUID userId, GameType gameType) {
        return repository.findTop20ByGameTypeAndWhitePlayerIdOrGameTypeAndBlackPlayerIdOrderByFinishedAtDesc(
                gameType, userId, gameType, userId);
    }
}
