package com.duelo64.backend.game.stats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.duelo64.backend.game.match.GameMatchRepository;
import com.duelo64.backend.game.match.MatchResult;
import com.duelo64.backend.game.room.GameType;
import com.duelo64.backend.user.User;

class PlayerStatsServiceTest {
    @Test
    void newPlayerHasInitialEloAndEmptyStats() {
        GameMatchRepository matches = mock(GameMatchRepository.class);
        PlayerGameRatingRepository ratings = mock(PlayerGameRatingRepository.class);
        UUID userId = UUID.randomUUID();
        when(ratings.findByUserIdAndGameType(userId, GameType.CHECKERS)).thenReturn(Optional.empty());

        PlayerStatsResponse response = new PlayerStatsService(matches, ratings)
                .statsFor(userId, GameType.CHECKERS);

        assertThat(response.rating()).isEqualTo(1000);
        assertThat(response.gamesPlayed()).isZero();
        assertThat(response.winRate()).isZero();
    }

    @Test
    void playerWithoutChessMatchesHasIndependentInitialStats() {
        GameMatchRepository matches = mock(GameMatchRepository.class);
        PlayerGameRatingRepository ratings = mock(PlayerGameRatingRepository.class);
        UUID userId = UUID.randomUUID();
        when(ratings.findByUserIdAndGameType(userId, GameType.CHESS)).thenReturn(Optional.empty());

        PlayerStatsResponse response = new PlayerStatsService(matches, ratings)
                .statsFor(userId, GameType.CHESS);

        assertThat(response.gameType()).isEqualTo(GameType.CHESS);
        assertThat(response.rating()).isEqualTo(1000);
        assertThat(response.gamesPlayed()).isZero();
        assertThat(response.wins()).isZero();
        assertThat(response.losses()).isZero();
        assertThat(response.draws()).isZero();
        assertThat(response.winRate()).isZero();
        verify(matches).countPlayedBy(userId, GameType.CHESS);
        verify(matches, never()).countPlayedBy(userId, GameType.CHECKERS);
    }

    @Test
    void generalStatsComeFromHistoryAndWinRateIsCalculated() {
        GameMatchRepository matches = mock(GameMatchRepository.class);
        PlayerGameRatingRepository ratings = mock(PlayerGameRatingRepository.class);
        UUID userId = UUID.randomUUID();
        when(matches.countPlayedBy(userId, GameType.CHECKERS)).thenReturn(14L);
        when(matches.countWonBy(userId, GameType.CHECKERS)).thenReturn(8L);
        when(matches.countLostBy(userId, GameType.CHECKERS)).thenReturn(4L);
        when(matches.countDrawnBy(userId, GameType.CHECKERS, MatchResult.DRAW)).thenReturn(2L);
        PlayerGameRating rating = new PlayerGameRating(mock(User.class), GameType.CHECKERS);
        rating.updateRating(1124);
        when(ratings.findByUserIdAndGameType(userId, GameType.CHECKERS)).thenReturn(Optional.of(rating));

        PlayerStatsResponse response = new PlayerStatsService(matches, ratings)
                .statsFor(userId, GameType.CHECKERS);

        assertThat(response.rating()).isEqualTo(1124);
        assertThat(response.gamesPlayed()).isEqualTo(14);
        assertThat(response.wins()).isEqualTo(8);
        assertThat(response.losses()).isEqualTo(4);
        assertThat(response.draws()).isEqualTo(2);
        assertThat(response.winRate()).isEqualTo(57.14);
    }
}
