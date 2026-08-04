package com.duelo64.backend.game.room;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRoomRepository extends JpaRepository<GameRoom, UUID> {

    @EntityGraph(attributePaths = { "host", "guest" })
    Optional<GameRoom> findByCode(String code);

    boolean existsByCode(String code);
}
