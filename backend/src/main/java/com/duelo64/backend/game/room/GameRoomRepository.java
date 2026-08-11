package com.duelo64.backend.game.room;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface GameRoomRepository extends JpaRepository<GameRoom, UUID> {

    @EntityGraph(attributePaths = { "host", "guest" })
    Optional<GameRoom> findByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select room from GameRoom room where room.code = :code")
    Optional<GameRoom> findByCodeForUpdate(@Param("code") String code);

    boolean existsByCode(String code);

    @EntityGraph(attributePaths = { "host", "guest" })
    @Query("select room from GameRoom room where room.status = :status and "
            + "(room.host.id = :userId or room.guest.id = :userId) order by room.startedAt desc")
    List<GameRoom> findActiveRoomsForUser(
            @Param("userId") UUID userId,
            @Param("status") RoomStatus status);
}
