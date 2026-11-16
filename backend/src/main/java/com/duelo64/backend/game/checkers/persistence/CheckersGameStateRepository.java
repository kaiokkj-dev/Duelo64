package com.duelo64.backend.game.checkers.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface CheckersGameStateRepository extends JpaRepository<CheckersGameState, UUID> {

    @EntityGraph(attributePaths = "room")
    Optional<CheckersGameState> findByRoomCode(String code);

    /**
     * Carrega o estado travando a linha ate o fim da transacao, para que lances,
     * desistencias, empates e timeouts simultaneos da mesma partida sejam serializados.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select state from CheckersGameState state where state.room.code = :code")
    Optional<CheckersGameState> findByRoomCodeForUpdate(@Param("code") String code);

    Optional<CheckersGameState> findByRoomId(UUID roomId);

    boolean existsByRoomId(UUID roomId);
}
