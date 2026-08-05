package com.duelo64.backend.game.checkers.persistence;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.duelo64.backend.game.checkers.domain.CheckersBoard;
import com.duelo64.backend.game.checkers.domain.CheckersMoveResult;
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

    @Column(name = "white_remaining_millis", nullable = false)
    private long whiteRemainingMillis;

    @Column(name = "black_remaining_millis", nullable = false)
    private long blackRemainingMillis;

    @Column(name = "turn_started_at")
    private Instant turnStartedAt;

    @Column(name = "forced_capture_row")
    private Integer forcedCaptureRow;

    @Column(name = "forced_capture_column")
    private Integer forcedCaptureColumn;

    @Enumerated(EnumType.STRING)
    @Column(name = "winner_color", length = 16)
    private PieceColor winnerColor;

    @Enumerated(EnumType.STRING)
    @Column(name = "finish_reason", length = 32)
    private CheckersFinishReason finishReason;

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

        long initialTimeMillis =
                room.getTimeControlMinutes() * 60_000L;

        this.whiteRemainingMillis = initialTimeMillis;
        this.blackRemainingMillis = initialTimeMillis;
        this.turnStartedAt = null;
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

    public void startClock(Instant startedAt) {
        if (this.turnStartedAt == null) {
            this.turnStartedAt = startedAt;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void consumeCurrentTurnTime(Instant now) {
        if (turnStartedAt == null) {
            turnStartedAt = now;
            return;
        }

        long elapsedMillis = Math.max(
                0,
                Duration.between(turnStartedAt, now).toMillis()
        );

        if (currentTurn == PieceColor.WHITE) {
            whiteRemainingMillis = Math.max(
                    0,
                    whiteRemainingMillis - elapsedMillis
            );
        } else {
            blackRemainingMillis = Math.max(
                    0,
                    blackRemainingMillis - elapsedMillis
            );
        }

        turnStartedAt = now;
    }

    public void apply(CheckersMoveResult result, Instant now) {
        this.boardNotation = result.board().toNotation();
        this.moveCount++;
        this.turnStartedAt = now;

        if (result.mustContinueCapture()) {
            this.forcedCaptureRow = result.landingPosition().row();
            this.forcedCaptureColumn = result.landingPosition().column();
            return;
        }

        this.forcedCaptureRow = null;
        this.forcedCaptureColumn = null;
        this.currentTurn = currentTurn.opponent();
    }

    public void finish(PieceColor winnerColor, CheckersFinishReason finishReason) {
        this.winnerColor = winnerColor;
        this.finishReason = finishReason;
        this.forcedCaptureRow = null;
        this.forcedCaptureColumn = null;
        this.turnStartedAt = null;
    }

    public boolean hasCurrentPlayerRunOutOfTime() {
        return getCurrentTurnRemainingMillis() <= 0;
    }

    public long getCurrentTurnRemainingMillis() {
        return currentTurn == PieceColor.WHITE
                ? whiteRemainingMillis
                : blackRemainingMillis;
    }

    public long calculateWhiteRemainingMillis(Instant now) {
        if (currentTurn != PieceColor.WHITE || turnStartedAt == null) {
            return whiteRemainingMillis;
        }

        long elapsedMillis = Math.max(
                0,
                Duration.between(turnStartedAt, now).toMillis()
        );

        return Math.max(0, whiteRemainingMillis - elapsedMillis);
    }

    public long calculateBlackRemainingMillis(Instant now) {
        if (currentTurn != PieceColor.BLACK || turnStartedAt == null) {
            return blackRemainingMillis;
        }

        long elapsedMillis = Math.max(
                0,
                Duration.between(turnStartedAt, now).toMillis()
        );

        return Math.max(0, blackRemainingMillis - elapsedMillis);
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

    public long getWhiteRemainingMillis() {
        return whiteRemainingMillis;
    }

    public long getBlackRemainingMillis() {
        return blackRemainingMillis;
    }

    public Instant getTurnStartedAt() {
        return turnStartedAt;
    }

    public boolean mustContinueCapture() {
        return forcedCaptureRow != null && forcedCaptureColumn != null;
    }

    public Integer getForcedCaptureRow() {
        return forcedCaptureRow;
    }

    public Integer getForcedCaptureColumn() {
        return forcedCaptureColumn;
    }

    public PieceColor getWinnerColor() {
        return winnerColor;
    }

    public CheckersFinishReason getFinishReason() {
        return finishReason;
    }

}
