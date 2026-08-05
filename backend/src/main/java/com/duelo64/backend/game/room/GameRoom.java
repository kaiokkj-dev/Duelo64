package com.duelo64.backend.game.room;

import java.time.Instant;
import java.util.UUID;

import com.duelo64.backend.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "game_rooms")
public class GameRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 6)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_type", nullable = false, length = 32)
    private GameType gameType;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false, length = 32)
    private RoomType roomType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RoomStatus status;

    @Column(name = "time_control_minutes", nullable = false)
    private int timeControlMinutes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_user_id", nullable = false)
    private User host;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_user_id")
    private User guest;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    protected GameRoom() {
    }

    private GameRoom(String code, GameType gameType, RoomType roomType, int timeControlMinutes, User host) {
        this.code = code;
        this.gameType = gameType;
        this.roomType = roomType;
        this.timeControlMinutes = timeControlMinutes;
        this.host = host;
        this.status = RoomStatus.WAITING;
    }

    public static GameRoom privateCheckers(String code, int timeControlMinutes, User host) {
        return new GameRoom(code, GameType.CHECKERS, RoomType.PRIVATE, timeControlMinutes, host);
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void join(User guest) {
        this.guest = guest;
        this.status = RoomStatus.IN_PROGRESS;
        this.startedAt = Instant.now();
    }

    public void finish() {
        if (status == RoomStatus.FINISHED) {
            return;
        }

        this.status = RoomStatus.FINISHED;
        this.finishedAt = Instant.now();
    }

    public boolean isFull() {
        return guest != null;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public GameType getGameType() {
        return gameType;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public int getTimeControlMinutes() {
        return timeControlMinutes;
    }

    public User getHost() {
        return host;
    }

    public User getGuest() {
        return guest;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }
}
