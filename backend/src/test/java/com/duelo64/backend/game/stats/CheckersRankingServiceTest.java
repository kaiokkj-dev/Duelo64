package com.duelo64.backend.game.stats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.duelo64.backend.game.room.GameType;

class CheckersRankingServiceTest {

    @Test
    void paginationAddsAbsolutePositionsAndCalculatesRankedWinRate() {
        PlayerGameRatingRepository repository = mock(PlayerGameRatingRepository.class);
        CheckersRankingProjection first = entry(UUID.randomUUID(), 1200, 10, 6, 3, 1);
        CheckersRankingProjection second = entry(UUID.randomUUID(), 1180, 4, 2, 2, 0);
        when(repository.findRanking("CHECKERS", PageRequest.of(1, 50)))
                .thenReturn(new PageImpl<>(List.of(first, second), PageRequest.of(1, 50), 120));

        CheckersRankingPageResponse response = new CheckersRankingService(repository).ranking(1);

        assertThat(response.content()).extracting(CheckersRankingEntryResponse::position)
                .containsExactly(51L, 52L);
        assertThat(response.content().get(0).rankedWinRate()).isEqualTo(60.0);
        assertThat(response.totalPages()).isEqualTo(3);
    }

    @Test
    void userWithoutRankedGamesHasNoPositionAndKeepsInitialRating() {
        PlayerGameRatingRepository repository = mock(PlayerGameRatingRepository.class);
        UUID userId = UUID.randomUUID();
        when(repository.findRankingPosition("CHECKERS", userId)).thenReturn(Optional.empty());
        when(repository.findByUserIdAndGameType(userId, com.duelo64.backend.game.room.GameType.CHECKERS))
                .thenReturn(Optional.empty());

        CheckersRankingMeResponse response = new CheckersRankingService(repository).positionFor(userId);

        assertThat(response.position()).isNull();
        assertThat(response.rating()).isEqualTo(1000);
    }

    @Test
    void chessRankingUsesOnlyChessRatingAndStatisticsQuery() {
        PlayerGameRatingRepository repository = mock(PlayerGameRatingRepository.class);
        CheckersRankingProjection player = entry(UUID.randomUUID(), 1320, 8, 5, 2, 1);
        when(repository.findRanking("CHESS", PageRequest.of(0, 50)))
                .thenReturn(new PageImpl<>(List.of(player), PageRequest.of(0, 50), 1));

        CheckersRankingPageResponse response = new CheckersRankingService(repository).ranking(GameType.CHESS, 0);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).rating()).isEqualTo(1320);
        assertThat(response.content().get(0).rankedWinRate()).isEqualTo(62.5);
    }

    private CheckersRankingProjection entry(
            UUID userId, int rating, long games, long wins, long losses, long draws) {
        CheckersRankingProjection item = mock(CheckersRankingProjection.class);
        when(item.getUserId()).thenReturn(userId);
        when(item.getNickname()).thenReturn("jogador");
        when(item.getRating()).thenReturn(rating);
        when(item.getRankedGames()).thenReturn(games);
        when(item.getRankedWins()).thenReturn(wins);
        when(item.getRankedLosses()).thenReturn(losses);
        when(item.getRankedDraws()).thenReturn(draws);
        return item;
    }
}
