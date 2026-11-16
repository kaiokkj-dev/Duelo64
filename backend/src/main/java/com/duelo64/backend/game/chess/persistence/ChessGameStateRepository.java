package com.duelo64.backend.game.chess.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface ChessGameStateRepository extends JpaRepository<ChessGameState, UUID> {
    boolean existsByRoomId(UUID roomId);
    Optional<ChessGameState> findByRoomId(UUID roomId);
    Optional<ChessGameState> findByRoomCode(String roomCode);

    /**
     * Carrega o estado travando a linha ate o fim da transacao, para que lances,
     * desistencias, empates e timeouts simultaneos da mesma partida sejam serializados.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select state from ChessGameState state where state.room.code = :code")
    Optional<ChessGameState> findByRoomCodeForUpdate(@Param("code") String code);
}
