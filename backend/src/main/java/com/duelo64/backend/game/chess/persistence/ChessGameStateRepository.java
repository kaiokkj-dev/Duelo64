package com.duelo64.backend.game.chess.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChessGameStateRepository extends JpaRepository<ChessGameState, UUID> {
    boolean existsByRoomId(UUID roomId);
    Optional<ChessGameState> findByRoomId(UUID roomId);
    Optional<ChessGameState> findByRoomCode(String roomCode);
}
