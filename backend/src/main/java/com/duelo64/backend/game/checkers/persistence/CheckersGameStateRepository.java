package com.duelo64.backend.game.checkers.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckersGameStateRepository extends JpaRepository<CheckersGameState, UUID> {

    @EntityGraph(attributePaths = "room")
    Optional<CheckersGameState> findByRoomCode(String code);

    Optional<CheckersGameState> findByRoomId(UUID roomId);

    boolean existsByRoomId(UUID roomId);
}
