package com.duelo64.backend.game.stats;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.duelo64.backend.game.match.GameMatch;
import com.duelo64.backend.game.match.MatchResult;
import com.duelo64.backend.game.room.MatchType;

@Service
public class EloRatingService {
    static final int K_FACTOR = 32;
    private final PlayerGameRatingRepository repository;

    public EloRatingService(PlayerGameRatingRepository repository) {
        this.repository = repository;
    }

    public void process(GameMatch match) {
        if (match.isEloProcessed()) return;
        if (match.getMatchType() == MatchType.FRIENDLY) {
            match.markEloProcessed();
            return;
        }

        PlayerGameRating white = ratingFor(match.getWhitePlayer().getId(), match.getWhitePlayer(), match.getGameType());
        PlayerGameRating black = ratingFor(match.getBlackPlayer().getId(), match.getBlackPlayer(), match.getGameType());
        int whiteBefore = white.getRating();
        int blackBefore = black.getRating();
        double whiteScore = match.getResult() == MatchResult.DRAW ? 0.5
                : match.getResult() == MatchResult.WHITE_WIN ? 1.0 : 0.0;
        double blackScore = 1.0 - whiteScore;

        int whiteAfter = calculate(whiteBefore, blackBefore, whiteScore);
        int blackAfter = calculate(blackBefore, whiteBefore, blackScore);
        white.updateRating(whiteAfter);
        black.updateRating(blackAfter);
        match.recordEloChange(whiteBefore, whiteAfter, blackBefore, blackAfter);
        match.markEloProcessed();
    }

    int calculate(int rating, int opponentRating, double score) {
        double expected = 1.0 / (1.0 + Math.pow(10.0, (opponentRating - rating) / 400.0));
        return (int) Math.round(rating + K_FACTOR * (score - expected));
    }

    private PlayerGameRating ratingFor(UUID userId, com.duelo64.backend.user.User user,
            com.duelo64.backend.game.room.GameType gameType) {
        return repository.findForUpdateByUserIdAndGameType(userId, gameType)
                .orElseGet(() -> repository.save(new PlayerGameRating(user, gameType)));
    }
}
