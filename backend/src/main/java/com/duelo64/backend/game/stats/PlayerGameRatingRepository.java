package com.duelo64.backend.game.stats;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.duelo64.backend.game.room.GameType;

import jakarta.persistence.LockModeType;

public interface PlayerGameRatingRepository extends JpaRepository<PlayerGameRating, UUID> {
    Optional<PlayerGameRating> findByUserIdAndGameType(UUID userId, GameType gameType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PlayerGameRating> findForUpdateByUserIdAndGameType(UUID userId, GameType gameType);

    @Query(value = """
            SELECT r.user_id AS "userId", u.nickname AS nickname, u.avatar_url AS "avatarUrl",
                   r.rating AS rating, COUNT(m.id) AS "rankedGames",
                   SUM(CASE WHEN m.winner_id = r.user_id THEN 1 ELSE 0 END) AS "rankedWins",
                   SUM(CASE WHEN m.loser_id = r.user_id THEN 1 ELSE 0 END) AS "rankedLosses",
                   SUM(CASE WHEN m.result = 'DRAW' THEN 1 ELSE 0 END) AS "rankedDraws"
            FROM player_game_ratings r
            JOIN users u ON u.id = r.user_id
            JOIN game_matches m ON m.match_type = 'RANKED'
                 AND m.game_type = r.game_type
                 AND (m.white_player_id = r.user_id OR m.black_player_id = r.user_id)
            WHERE r.game_type = :gameType
            GROUP BY r.user_id, u.nickname, u.avatar_url, r.rating
            ORDER BY r.rating DESC, COUNT(m.id) DESC, r.user_id ASC
            """,
            countQuery = """
            SELECT COUNT(*) FROM player_game_ratings r
            WHERE r.game_type = :gameType
              AND EXISTS (
                  SELECT 1 FROM game_matches m
                  WHERE m.match_type = 'RANKED'
                    AND m.game_type = r.game_type
                    AND (m.white_player_id = r.user_id OR m.black_player_id = r.user_id)
              )
            """,
            nativeQuery = true)
    Page<CheckersRankingProjection> findRanking(@Param("gameType") String gameType, Pageable pageable);

    @Query(value = """
            SELECT ranked.position AS position, ranked.rating AS rating
            FROM (
                SELECT r.user_id,
                       r.rating,
                       ROW_NUMBER() OVER (
                           ORDER BY r.rating DESC, COUNT(m.id) DESC, r.user_id ASC
                       ) AS position
                FROM player_game_ratings r
                JOIN game_matches m ON m.match_type = 'RANKED'
                     AND m.game_type = r.game_type
                     AND (m.white_player_id = r.user_id OR m.black_player_id = r.user_id)
                WHERE r.game_type = :gameType
                GROUP BY r.user_id, r.rating
            ) ranked
            WHERE ranked.user_id = :userId
            """, nativeQuery = true)
    Optional<CheckersRankingPositionProjection> findRankingPosition(
            @Param("gameType") String gameType,
            @Param("userId") UUID userId);
}
