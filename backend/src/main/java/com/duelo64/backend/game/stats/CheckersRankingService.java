package com.duelo64.backend.game.stats;

import java.util.UUID;
import java.util.stream.IntStream;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.duelo64.backend.game.room.GameType;

@Service
public class CheckersRankingService {
    static final int PAGE_SIZE = 50;
    private final PlayerGameRatingRepository repository;

    public CheckersRankingService(PlayerGameRatingRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public CheckersRankingPageResponse ranking(int requestedPage) {
        return ranking(GameType.CHECKERS, requestedPage);
    }

    @Transactional(readOnly = true)
    public CheckersRankingPageResponse ranking(GameType gameType, int requestedPage) {
        int pageNumber = Math.max(0, requestedPage);
        var page = repository.findRanking(gameType.name(), PageRequest.of(pageNumber, PAGE_SIZE));
        long offset = page.getPageable().getOffset();
        var pageItems = page.getContent();
        var content = IntStream.range(0, pageItems.size())
                .mapToObj(index -> toResponse(pageItems.get(index), offset + index + 1))
                .toList();
        return new CheckersRankingPageResponse(
                content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public CheckersRankingMeResponse positionFor(UUID userId) {
        return positionFor(GameType.CHECKERS, userId);
    }

    @Transactional(readOnly = true)
    public CheckersRankingMeResponse positionFor(GameType gameType, UUID userId) {
        return repository.findRankingPosition(gameType.name(), userId)
                .map(item -> new CheckersRankingMeResponse(item.getPosition(), item.getRating()))
                .orElseGet(() -> new CheckersRankingMeResponse(
                        null,
                        repository.findByUserIdAndGameType(userId, gameType)
                                .map(PlayerGameRating::getRating)
                                .orElse(PlayerGameRating.INITIAL_RATING)));
    }

    private CheckersRankingEntryResponse toResponse(CheckersRankingProjection item, long position) {
        long games = item.getRankedGames();
        double winRate = games == 0 ? 0.0 : Math.round(item.getRankedWins() * 10000.0 / games) / 100.0;
        return new CheckersRankingEntryResponse(
                position,
                item.getUserId(),
                item.getNickname(),
                item.getAvatarUrl(),
                item.getRating(),
                games,
                item.getRankedWins(),
                item.getRankedLosses(),
                item.getRankedDraws(),
                winRate);
    }
}
