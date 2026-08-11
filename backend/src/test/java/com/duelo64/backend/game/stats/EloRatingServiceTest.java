package com.duelo64.backend.game.stats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.duelo64.backend.game.checkers.domain.PieceColor;
import com.duelo64.backend.game.checkers.persistence.CheckersFinishReason;
import com.duelo64.backend.game.checkers.persistence.CheckersGameState;
import com.duelo64.backend.game.match.GameMatch;
import com.duelo64.backend.game.match.CompletedMatchSnapshot;
import com.duelo64.backend.game.match.MatchResult;
import com.duelo64.backend.game.room.GameRoom;
import com.duelo64.backend.game.room.GameType;
import com.duelo64.backend.user.User;

class EloRatingServiceTest {
    @Test
    void newPlayersStartAt1000AndRankedWinUpdatesBothRatings() {
        PlayerGameRatingRepository repository = repositoryCreatingRatings();
        EloRatingService service = new EloRatingService(repository);
        GameMatch match = rankedMatch(PieceColor.WHITE);

        service.process(match);

        PlayerGameRating white = ratingArgument(repository, 0);
        PlayerGameRating black = ratingArgument(repository, 1);
        assertThat(white.getRating()).isEqualTo(1016);
        assertThat(black.getRating()).isEqualTo(984);
        assertThat(match.getWhiteRatingBefore()).isEqualTo(1000);
        assertThat(match.getWhiteRatingAfter()).isEqualTo(1016);
        assertThat(match.getBlackRatingBefore()).isEqualTo(1000);
        assertThat(match.getBlackRatingAfter()).isEqualTo(984);
        assertThat(match.isEloProcessed()).isTrue();
    }

    @Test
    void rankedDrawUsesPreviousRatingsAndScoreOfHalf() {
        GameMatch match = rankedMatch(null);
        PlayerGameRating white = new PlayerGameRating(match.getWhitePlayer(), GameType.CHECKERS);
        PlayerGameRating black = new PlayerGameRating(match.getBlackPlayer(), GameType.CHECKERS);
        white.updateRating(1200);
        black.updateRating(800);
        PlayerGameRatingRepository repository = mock(PlayerGameRatingRepository.class);
        when(repository.findForUpdateByUserIdAndGameType(match.getWhitePlayer().getId(), GameType.CHECKERS))
                .thenReturn(Optional.of(white));
        when(repository.findForUpdateByUserIdAndGameType(match.getBlackPlayer().getId(), GameType.CHECKERS))
                .thenReturn(Optional.of(black));

        new EloRatingService(repository).process(match);

        assertThat(white.getRating()).isEqualTo(1187);
        assertThat(black.getRating()).isEqualTo(813);
        assertThat(match.getWhiteRatingBefore()).isEqualTo(1200);
        assertThat(match.getWhiteRatingAfter()).isEqualTo(1187);
        assertThat(match.getBlackRatingBefore()).isEqualTo(800);
        assertThat(match.getBlackRatingAfter()).isEqualTo(813);
    }

    @Test
    void friendlyMatchDoesNotChangeEloAndMatchCannotBeProcessedTwice() {
        PlayerGameRatingRepository repository = mock(PlayerGameRatingRepository.class);
        EloRatingService service = new EloRatingService(repository);
        GameMatch match = friendlyMatch(PieceColor.BLACK);

        service.process(match);
        service.process(match);

        assertThat(match.isEloProcessed()).isTrue();
        assertThat(match.getWhiteRatingBefore()).isNull();
        assertThat(match.getBlackRatingBefore()).isNull();
        verifyNoInteractions(repository);
    }

    @Test
    void processingSameRankedMatchTwicePreservesOriginalSnapshot() {
        PlayerGameRatingRepository repository = repositoryCreatingRatings();
        EloRatingService service = new EloRatingService(repository);
        GameMatch match = rankedMatch(PieceColor.WHITE);

        service.process(match);
        service.process(match);

        assertThat(match.getWhiteRatingBefore()).isEqualTo(1000);
        assertThat(match.getWhiteRatingAfter()).isEqualTo(1016);
        verify(repository, times(2)).save(any(PlayerGameRating.class));
    }

    @Test
    void rankedChessMatchUpdatesOnlyChessRating() {
        PlayerGameRatingRepository repository = repositoryCreatingRatings();
        GameMatch match = rankedMatch(GameType.CHESS, PieceColor.WHITE);

        new EloRatingService(repository).process(match);

        verify(repository).findForUpdateByUserIdAndGameType(match.getWhitePlayer().getId(), GameType.CHESS);
        verify(repository).findForUpdateByUserIdAndGameType(match.getBlackPlayer().getId(), GameType.CHESS);
        verify(repository, never()).findForUpdateByUserIdAndGameType(any(UUID.class), eq(GameType.CHECKERS));
        assertThat(match.getWhiteRatingAfter()).isEqualTo(1016);
        assertThat(match.getBlackRatingAfter()).isEqualTo(984);
    }

    @SuppressWarnings("unchecked")
    private PlayerGameRating ratingArgument(PlayerGameRatingRepository repository, int index) {
        var captor = org.mockito.ArgumentCaptor.forClass(PlayerGameRating.class);
        verify(repository, times(2)).save(captor.capture());
        return captor.getAllValues().get(index);
    }

    private PlayerGameRatingRepository repositoryCreatingRatings() {
        PlayerGameRatingRepository repository = mock(PlayerGameRatingRepository.class);
        when(repository.findForUpdateByUserIdAndGameType(any(UUID.class), any(GameType.class)))
                .thenReturn(Optional.empty());
        when(repository.save(any(PlayerGameRating.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return repository;
    }

    private GameMatch rankedMatch(PieceColor winner) {
        return rankedMatch(GameType.CHECKERS, winner);
    }

    private GameMatch rankedMatch(GameType gameType, PieceColor winner) {
        return match(GameRoom.rankedRoom("RANK01", gameType, 10, user("white")), winner);
    }

    private GameMatch friendlyMatch(PieceColor winner) {
        return match(GameRoom.privateCheckers("FRND01", 10, user("white")), winner);
    }

    private GameMatch match(GameRoom room, PieceColor winner) {
        ReflectionTestUtils.setField(room, "id", UUID.randomUUID());
        room.join(user("black"));
        Instant started = Instant.parse("2026-08-10T18:00:00Z");
        ReflectionTestUtils.setField(room, "startedAt", started);
        CheckersGameState state = CheckersGameState.start(room);
        state.finish(winner, winner == null ? CheckersFinishReason.DRAW_AGREEMENT : CheckersFinishReason.NO_PIECES);
        room.finish();
        ReflectionTestUtils.setField(room, "finishedAt", started.plusSeconds(300));
        MatchResult result = winner == null ? MatchResult.DRAW
                : winner == PieceColor.WHITE ? MatchResult.WHITE_WIN : MatchResult.BLACK_WIN;
        String reason = (winner == null
                ? CheckersFinishReason.DRAW_AGREEMENT
                : CheckersFinishReason.NO_PIECES).name();
        return GameMatch.from(new CompletedMatchSnapshot(room, result, reason, state.getMoveCount()));
    }

    private User user(String nickname) {
        User user = new User(nickname + "@duelo64.com");
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        user.updateNickname(nickname);
        return user;
    }
}
