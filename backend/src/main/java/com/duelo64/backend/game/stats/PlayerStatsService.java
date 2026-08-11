package com.duelo64.backend.game.stats;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.duelo64.backend.game.match.GameMatchRepository;
import com.duelo64.backend.game.match.MatchResult;
import com.duelo64.backend.game.room.GameType;

@Service
public class PlayerStatsService {
    private final GameMatchRepository matchRepository;
    private final PlayerGameRatingRepository ratingRepository;

    public PlayerStatsService(GameMatchRepository matchRepository, PlayerGameRatingRepository ratingRepository) {
        this.matchRepository = matchRepository;
        this.ratingRepository = ratingRepository;
    }

    @Transactional(readOnly = true)
    public PlayerStatsResponse statsFor(UUID userId, GameType gameType) {
        long games = matchRepository.countPlayedBy(userId, gameType);
        long wins = matchRepository.countWonBy(userId, gameType);
        long losses = matchRepository.countLostBy(userId, gameType);
        long draws = matchRepository.countDrawnBy(userId, gameType, MatchResult.DRAW);
        int rating = ratingRepository.findByUserIdAndGameType(userId, gameType)
                .map(PlayerGameRating::getRating)
                .orElse(PlayerGameRating.INITIAL_RATING);
        double winRate = games == 0 ? 0.0 : Math.round((wins * 10000.0) / games) / 100.0;
        return new PlayerStatsResponse(gameType, rating, games, wins, losses, draws, winRate);
    }
}
