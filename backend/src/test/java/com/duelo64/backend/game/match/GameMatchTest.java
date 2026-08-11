package com.duelo64.backend.game.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import com.duelo64.backend.game.checkers.domain.PieceColor;
import com.duelo64.backend.game.checkers.persistence.CheckersFinishReason;
import com.duelo64.backend.game.checkers.persistence.CheckersGameState;
import com.duelo64.backend.game.room.GameRoom;
import com.duelo64.backend.game.room.GameType;
import com.duelo64.backend.game.stats.EloRatingService;
import com.duelo64.backend.user.User;

class GameMatchTest {
    @Test
    void whiteVictoryAndBlackVictoryCreateCorrectResults() {
        assertThat(match(PieceColor.WHITE, CheckersFinishReason.TIMEOUT).getResult()).isEqualTo(MatchResult.WHITE_WIN);
        GameMatch blackWin = match(PieceColor.BLACK, CheckersFinishReason.RESIGNATION);
        assertThat(blackWin.getResult()).isEqualTo(MatchResult.BLACK_WIN);
        assertThat(blackWin.getFinishReason()).isEqualTo(CheckersFinishReason.RESIGNATION.name());
    }

    @Test
    void everyDrawReasonCreatesHistoryWithoutWinnerOrLoser() {
        for (CheckersFinishReason reason : List.of(
                CheckersFinishReason.DRAW_AGREEMENT,
                CheckersFinishReason.DRAW_REPETITION,
                CheckersFinishReason.DRAW_MOVE_LIMIT)) {
            GameMatch match = match(null, reason);
            assertThat(match.getResult()).isEqualTo(MatchResult.DRAW);
            assertThat(match.getWinner()).isNull();
            assertThat(match.getLoser()).isNull();
            assertThat(match.getFinishReason()).isEqualTo(reason.name());
        }
    }

    @Test
    void recordingSameRoomTwiceIsIdempotent() {
        GameMatchRepository repository = mock(GameMatchRepository.class);
        EloRatingService eloRatingService = mock(EloRatingService.class);
        MatchHistoryService service = new MatchHistoryService(repository, eloRatingService);
        CompletedMatchSnapshot snapshot = completedSnapshot(PieceColor.WHITE, CheckersFinishReason.NO_PIECES);
        when(repository.existsByRoomId(snapshot.room().getId())).thenReturn(false, true);

        service.recordCompletedMatch(snapshot);
        service.recordCompletedMatch(snapshot);

        verify(repository, org.mockito.Mockito.times(1)).save(org.mockito.ArgumentMatchers.any(GameMatch.class));
        verify(eloRatingService, org.mockito.Mockito.times(1))
                .process(org.mockito.ArgumentMatchers.any(GameMatch.class));
    }

    @Test
    void historyQueryIsScopedToAuthenticatedUser() {
        GameMatchRepository repository = mock(GameMatchRepository.class);
        MatchHistoryService service = new MatchHistoryService(repository, mock(EloRatingService.class));
        UUID userId = UUID.randomUUID();
        when(repository.findTop20ByWhitePlayerIdOrBlackPlayerIdOrderByFinishedAtDesc(userId, userId))
                .thenReturn(List.of());

        service.historyFor(userId);

        verify(repository).findTop20ByWhitePlayerIdOrBlackPlayerIdOrderByFinishedAtDesc(userId, userId);
    }

    @Test
    void historyQueryCanBeFilteredByGameType() {
        GameMatchRepository repository = mock(GameMatchRepository.class);
        MatchHistoryService service = new MatchHistoryService(repository, mock(EloRatingService.class));
        UUID userId = UUID.randomUUID();
        when(repository.findTop20ByGameTypeAndWhitePlayerIdOrGameTypeAndBlackPlayerIdOrderByFinishedAtDesc(
                GameType.CHESS, userId, GameType.CHESS, userId)).thenReturn(List.of());

        service.historyFor(userId, GameType.CHESS);

        verify(repository).findTop20ByGameTypeAndWhitePlayerIdOrGameTypeAndBlackPlayerIdOrderByFinishedAtDesc(
                GameType.CHESS, userId, GameType.CHESS, userId);
        verify(repository, org.mockito.Mockito.never())
                .findTop20ByWhitePlayerIdOrBlackPlayerIdOrderByFinishedAtDesc(userId, userId);
    }

    @Test
    void responseReturnsOpponentAndPlayersColor() {
        GameMatch match = match(PieceColor.WHITE, CheckersFinishReason.NO_LEGAL_MOVES);
        UUID whiteId = match.getWhitePlayer().getId();

        MatchHistoryItemResponse response = MatchHistoryItemResponse.from(match, whiteId);

        assertThat(response.playerColor()).isEqualTo("WHITE");
        assertThat(response.result()).isEqualTo("WIN");
        assertThat(response.opponentId()).isEqualTo(match.getBlackPlayer().getId());
        assertThat(response.ratingChange()).isNull();
    }

    @Test
    void responseReturnsHistoricalRatingChangeForEachRankedPlayer() {
        User white = user("brancas");
        User black = user("pretas");
        GameRoom room = GameRoom.rankedCheckers("RANK01", 10, white);
        ReflectionTestUtils.setField(room, "id", UUID.randomUUID());
        room.join(black);
        Instant started = Instant.parse("2026-08-10T18:00:00Z");
        ReflectionTestUtils.setField(room, "startedAt", started);
        CheckersGameState state = CheckersGameState.start(room);
        state.finish(PieceColor.WHITE, CheckersFinishReason.NO_PIECES);
        room.finish();
        ReflectionTestUtils.setField(room, "finishedAt", started.plusSeconds(300));
        GameMatch match = GameMatch.from(snapshot(state, PieceColor.WHITE, CheckersFinishReason.NO_PIECES));
        match.recordEloChange(1000, 1016, 1000, 984);

        assertThat(MatchHistoryItemResponse.from(match, white.getId()).ratingChange()).isEqualTo(16);
        assertThat(MatchHistoryItemResponse.from(match, black.getId()).ratingChange()).isEqualTo(-16);
    }

    private GameMatch match(PieceColor winner, CheckersFinishReason reason) {
        return GameMatch.from(completedSnapshot(winner, reason));
    }

    private CompletedMatchSnapshot completedSnapshot(PieceColor winner, CheckersFinishReason reason) {
        CheckersGameState state = completedState(winner, reason);
        return snapshot(state, winner, reason);
    }

    private CompletedMatchSnapshot snapshot(
            CheckersGameState state, PieceColor winner, CheckersFinishReason reason) {
        MatchResult result = winner == null ? MatchResult.DRAW
                : winner == PieceColor.WHITE ? MatchResult.WHITE_WIN : MatchResult.BLACK_WIN;
        return new CompletedMatchSnapshot(state.getRoom(), result, reason.name(), state.getMoveCount());
    }

    private CheckersGameState completedState(PieceColor winner, CheckersFinishReason reason) {
        User white = user("brancas");
        User black = user("pretas");
        GameRoom room = GameRoom.privateCheckers("ABC123", 10, white);
        ReflectionTestUtils.setField(room, "id", UUID.randomUUID());
        room.join(black);
        Instant started = Instant.parse("2026-08-10T18:00:00Z");
        ReflectionTestUtils.setField(room, "startedAt", started);
        CheckersGameState state = CheckersGameState.start(room);
        ReflectionTestUtils.setField(state, "moveCount", 42);
        state.finish(winner, reason);
        room.finish();
        ReflectionTestUtils.setField(room, "finishedAt", started.plusSeconds(300));
        return state;
    }

    private User user(String nickname) {
        User user = new User(nickname + "@duelo64.com");
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        user.updateNickname(nickname);
        return user;
    }
}
