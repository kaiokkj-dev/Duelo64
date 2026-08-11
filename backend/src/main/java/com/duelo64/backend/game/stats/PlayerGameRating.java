package com.duelo64.backend.game.stats;

import java.time.Instant;
import java.util.UUID;

import com.duelo64.backend.game.room.GameType;
import com.duelo64.backend.user.User;

import jakarta.persistence.*;

@Entity
@Table(name = "player_game_ratings", uniqueConstraints = @UniqueConstraint(
        name = "uk_player_game_ratings_user_game", columnNames = { "user_id", "game_type" }))
public class PlayerGameRating {
    public static final int INITIAL_RATING = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_type", nullable = false, length = 32)
    private GameType gameType;

    @Column(nullable = false)
    private int rating;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PlayerGameRating() {}

    public PlayerGameRating(User user, GameType gameType) {
        this.user = user;
        this.gameType = gameType;
        this.rating = INITIAL_RATING;
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

    public void updateRating(int rating) { this.rating = Math.max(100, rating); }
    public UUID getId() { return id; }
    public User getUser() { return user; }
    public GameType getGameType() { return gameType; }
    public int getRating() { return rating; }
}
