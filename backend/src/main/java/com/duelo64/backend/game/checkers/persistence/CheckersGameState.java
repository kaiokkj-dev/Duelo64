package com.duelo64.backend.game.checkers.persistence;

import java.time.Instant;
import java.util.UUID;

import com.duelo64.backend.game.checkers.domain.CheckersBoard;
import com.duelo64.backend.game.checkers.domain.PieceColor;
import com.duelo64.backend.game.room.GameRoom;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "checkers_game_states")
public class CheckersGameState {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private GameRoom room;

    @Column(name = "board_notation", nullable = false, columnDefinition = "TEXT")
    private String boardNotation;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_turn", nullable = false, length = 16)
    private PieceColor currentTurn;

    @Column(name = "move_count", nullable = false)
    private int moveCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CheckersGameState() {
    }

    private CheckersGameState(GameRoom room) {
        this.room = room;
        this.boardNotation = CheckersBoard.initial().toNotation();
        this.currentTurn = PieceColor.WHITE;
        this.moveCount = 0;
    }

    public static CheckersGameState start(GameRoom room) {
        return new CheckersGameState(room);
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

    public void apply(CheckersBoard board) {
        this.boardNotation = board.toNotation();
        this.currentTurn = currentTurn.opponent();
        this.moveCount++;
    }

    public UUID getId() {
        return id;
    }

    public GameRoom getRoom() {
        return room;
    }

    public String getBoardNotation() {
        return boardNotation;
    }

    public PieceColor getCurrentTurn() {
        return currentTurn;
    }

    public int getMoveCount() {
        return moveCount;
    }
}
