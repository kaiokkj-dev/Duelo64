package com.duelo64.backend.game.match;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.duelo64.backend.game.room.GameType;

public interface GameMatchRepository extends JpaRepository<GameMatch, UUID> {
    boolean existsByRoomId(UUID roomId);

    @EntityGraph(attributePaths = { "whitePlayer", "blackPlayer", "winner", "loser" })
    List<GameMatch> findTop20ByWhitePlayerIdOrBlackPlayerIdOrderByFinishedAtDesc(UUID whiteId, UUID blackId);

    @EntityGraph(attributePaths = { "whitePlayer", "blackPlayer", "winner", "loser" })
    List<GameMatch> findTop20ByGameTypeAndWhitePlayerIdOrGameTypeAndBlackPlayerIdOrderByFinishedAtDesc(
            GameType whiteGameType, UUID whiteId, GameType blackGameType, UUID blackId);

    @Query("select count(m) from GameMatch m where m.gameType = :gameType and "
            + "(m.whitePlayer.id = :userId or m.blackPlayer.id = :userId)")
    long countPlayedBy(@Param("userId") UUID userId, @Param("gameType") GameType gameType);

    @Query("select count(m) from GameMatch m where m.gameType = :gameType and m.winner.id = :userId")
    long countWonBy(@Param("userId") UUID userId, @Param("gameType") GameType gameType);

    @Query("select count(m) from GameMatch m where m.gameType = :gameType and m.loser.id = :userId")
    long countLostBy(@Param("userId") UUID userId, @Param("gameType") GameType gameType);

    @Query("select count(m) from GameMatch m where m.gameType = :gameType and m.result = :result and "
            + "(m.whitePlayer.id = :userId or m.blackPlayer.id = :userId)")
    long countDrawnBy(@Param("userId") UUID userId, @Param("gameType") GameType gameType,
            @Param("result") MatchResult result);
}
