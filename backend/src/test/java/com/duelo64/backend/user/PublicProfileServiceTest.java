package com.duelo64.backend.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.duelo64.backend.game.match.GameMatch;
import com.duelo64.backend.game.match.MatchHistoryService;
import com.duelo64.backend.game.match.MatchResult;
import com.duelo64.backend.game.room.GameType;
import com.duelo64.backend.game.room.MatchType;
import com.duelo64.backend.game.stats.PlayerStatsResponse;
import com.duelo64.backend.game.stats.PlayerStatsService;

class PublicProfileServiceTest {

    @Test
    void publicProfileReusesStatsAndReturnsSafeRecentHistory() {
        User viewed = user("kaio");
        viewed.updateAvatarUrl("https://cdn.example/avatar.png");
        User opponent = user("rival");
        GameMatch match = rankedDraw(viewed, opponent);
        UserRepository users = mock(UserRepository.class);
        PlayerStatsService stats = mock(PlayerStatsService.class);
        MatchHistoryService history = mock(MatchHistoryService.class);
        when(users.findById(viewed.getId())).thenReturn(Optional.of(viewed));
        when(stats.statsFor(viewed.getId(), GameType.CHECKERS))
                .thenReturn(new PlayerStatsResponse(GameType.CHECKERS, 1042, 10, 5, 3, 2, 50.0));
        when(history.historyFor(viewed.getId(), GameType.CHECKERS)).thenReturn(List.of(match));

        PublicProfileResponse response = new PublicProfileService(users, stats, history).findById(viewed.getId());

        assertThat(response.nickname()).isEqualTo("kaio");
        assertThat(response.avatarUrl()).isEqualTo("https://cdn.example/avatar.png");
        assertThat(response.rating()).isEqualTo(1042);
        assertThat(response.gamesPlayed()).isEqualTo(10);
        assertThat(response.recentMatches()).singleElement()
                .satisfies(item -> {
                    assertThat(item.opponentId()).isEqualTo(opponent.getId());
                    assertThat(item.result()).isEqualTo("DRAW");
                    assertThat(item.ratingChange()).isEqualTo(2);
                });
        assertThat(Arrays.stream(PublicProfileResponse.class.getRecordComponents()).map(component -> component.getName()))
                .doesNotContain("email", "createdAt", "updatedAt", "lastLoginAt");
        assertThat(Arrays.stream(PublicMatchHistoryItemResponse.class.getRecordComponents()).map(component -> component.getName()))
                .doesNotContain("roomCode");
    }

    @Test
    void publicProfileCanSelectChessWithoutMixingCheckersStats() {
        User viewed = user("chessplayer");
        UserRepository users = mock(UserRepository.class);
        PlayerStatsService stats = mock(PlayerStatsService.class);
        MatchHistoryService history = mock(MatchHistoryService.class);
        when(users.findById(viewed.getId())).thenReturn(Optional.of(viewed));
        when(stats.statsFor(viewed.getId(), GameType.CHESS))
                .thenReturn(new PlayerStatsResponse(GameType.CHESS, 1088, 6, 4, 1, 1, 66.67));
        when(history.historyFor(viewed.getId(), GameType.CHESS)).thenReturn(List.of());

        PublicProfileResponse response = new PublicProfileService(users, stats, history)
                .findById(viewed.getId(), GameType.CHESS);

        assertThat(response.rating()).isEqualTo(1088);
        assertThat(response.gamesPlayed()).isEqualTo(6);
        assertThat(response.recentMatches()).isEmpty();
        org.mockito.Mockito.verify(stats).statsFor(viewed.getId(), GameType.CHESS);
        org.mockito.Mockito.verify(stats, org.mockito.Mockito.never())
                .statsFor(viewed.getId(), GameType.CHECKERS);
    }

    @Test
    void missingPlayerReturnsNotFound() {
        UserRepository users = mock(UserRepository.class);
        UUID userId = UUID.randomUUID();
        when(users.findById(userId)).thenReturn(Optional.empty());
        PublicProfileService service = new PublicProfileService(
                users, mock(PlayerStatsService.class), mock(MatchHistoryService.class));

        assertThatThrownBy(() -> service.findById(userId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Jogador nao encontrado");
    }

    private GameMatch rankedDraw(User white, User black) {
        GameMatch match = mock(GameMatch.class);
        when(match.getId()).thenReturn(UUID.randomUUID());
        when(match.getWhitePlayer()).thenReturn(white);
        when(match.getBlackPlayer()).thenReturn(black);
        when(match.getResult()).thenReturn(MatchResult.DRAW);
        when(match.getGameType()).thenReturn(GameType.CHECKERS);
        when(match.getMatchType()).thenReturn(MatchType.RANKED);
        when(match.getFinishReason()).thenReturn("DRAW_AGREEMENT");
        when(match.getTimeControlMinutes()).thenReturn(10);
        when(match.getFinishedAt()).thenReturn(Instant.parse("2026-08-10T18:00:00Z"));
        when(match.getMoveCount()).thenReturn(40);
        when(match.getWhiteRatingBefore()).thenReturn(1000);
        when(match.getWhiteRatingAfter()).thenReturn(1002);
        return match;
    }

    private User user(String nickname) {
        User user = new User(nickname + "@duelo64.com");
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        user.updateNickname(nickname);
        return user;
    }
}
