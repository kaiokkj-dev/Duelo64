package com.duelo64.backend.game.chess.persistence;

import java.time.Instant;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.duelo64.backend.game.chess.domain.*;
import com.duelo64.backend.game.room.GameRoom;
import com.duelo64.backend.game.room.GameType;

import jakarta.persistence.*;

@Entity
@Table(name = "chess_game_states")
public class ChessGameState {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false, unique = true)
    private GameRoom room;
    @Column(name = "board_fen", nullable = false, length = 160)
    private String fen;
    @Column(name = "move_count", nullable = false)
    private int moveCount;
    @Column(name = "white_remaining_millis", nullable = false)
    private long whiteRemainingMillis;
    @Column(name = "black_remaining_millis", nullable = false)
    private long blackRemainingMillis;
    @Column(name = "turn_started_at")
    private Instant turnStartedAt;
    @Column(name = "position_occurrences", nullable = false, columnDefinition = "TEXT")
    private String positionOccurrences;
    @Enumerated(EnumType.STRING) @Column(name = "winner_color", length = 16)
    private ChessColor winnerColor;
    @Enumerated(EnumType.STRING) @Column(name = "loser_color", length = 16)
    private ChessColor loserColor;
    @Enumerated(EnumType.STRING) @Column(name = "finish_reason", length = 40)
    private ChessFinishReason finishReason;
    @Column(name = "finished_at")
    private Instant finishedAt;
    @Enumerated(EnumType.STRING) @Column(name = "draw_offered_by_color", length = 16)
    private ChessColor drawOfferedByColor;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ChessGameState() {}

    private ChessGameState(GameRoom room) {
        if (room.getGameType() != GameType.CHESS) throw new IllegalArgumentException("O estado de Xadrez exige uma sala CHESS.");
        this.room = room;
        this.fen = ChessFen.initial().notation();
        long initialTime = room.getTimeControlMinutes() * 60_000L;
        this.whiteRemainingMillis = initialTime;
        this.blackRemainingMillis = initialTime;
        this.positionOccurrences = "";
        recordCurrentPosition(false);
    }

    public static ChessGameState start(GameRoom room) { return new ChessGameState(room); }

    public void apply(ChessMoveResult result) {
        ChessFen before = chessFen();
        int halfmove = result.capture() || result.pawnMove() ? 0 : before.halfmoveClock() + 1;
        int fullmove = before.fullmoveNumber() + (before.activeColor() == ChessColor.BLACK ? 1 : 0);
        this.fen = new ChessFen(
                result.board(), before.activeColor().opponent(), result.castlingRights(),
                result.enPassantTarget(), halfmove, fullmove).notation();
        this.moveCount++;
        this.drawOfferedByColor = null;
    }

    public void apply(ChessMoveResult result, Instant now) {
        apply(result);
        this.turnStartedAt = now;
    }

    public void startClock(Instant startedAt) {
        if (turnStartedAt == null) turnStartedAt = startedAt;
    }

    public void consumeCurrentTurnTime(Instant now) {
        if (turnStartedAt == null) { turnStartedAt = now; return; }
        long elapsed = Math.max(0, Duration.between(turnStartedAt, now).toMillis());
        if (getCurrentTurn() == ChessColor.WHITE) whiteRemainingMillis = Math.max(0, whiteRemainingMillis - elapsed);
        else blackRemainingMillis = Math.max(0, blackRemainingMillis - elapsed);
        turnStartedAt = now;
    }

    public long calculateWhiteRemainingMillis(Instant now) {
        if (getCurrentTurn() != ChessColor.WHITE || turnStartedAt == null) return whiteRemainingMillis;
        return Math.max(0, whiteRemainingMillis - Math.max(0, Duration.between(turnStartedAt, now).toMillis()));
    }

    public long calculateBlackRemainingMillis(Instant now) {
        if (getCurrentTurn() != ChessColor.BLACK || turnStartedAt == null) return blackRemainingMillis;
        return Math.max(0, blackRemainingMillis - Math.max(0, Duration.between(turnStartedAt, now).toMillis()));
    }

    public boolean hasCurrentPlayerRunOutOfTime() {
        return getCurrentTurn() == ChessColor.WHITE ? whiteRemainingMillis <= 0 : blackRemainingMillis <= 0;
    }

    public void offerDraw(ChessColor color) { drawOfferedByColor = color; }
    public void declineDraw() { drawOfferedByColor = null; }
    public boolean hasPendingDrawOffer() { return drawOfferedByColor != null; }

    public int recordCurrentPosition(boolean enPassantLegallyRelevant) {
        Map<String, Integer> occurrences = parseOccurrences();
        String identity = chessFen().repetitionIdentity(enPassantLegallyRelevant);
        int count = occurrences.getOrDefault(identity, 0) + 1;
        occurrences.put(identity, count);
        positionOccurrences = occurrences.entrySet().stream()
                .map(entry -> java.util.Base64.getUrlEncoder().withoutPadding()
                        .encodeToString(entry.getKey().getBytes(java.nio.charset.StandardCharsets.UTF_8))
                        + ":" + entry.getValue())
                .collect(Collectors.joining(","));
        return count;
    }

    public void finish(ChessColor winner, ChessFinishReason reason, Instant now) {
        if (finishReason != null) return;
        this.winnerColor = winner;
        this.loserColor = winner == null ? null : winner.opponent();
        this.finishReason = reason;
        this.finishedAt = now;
        this.turnStartedAt = null;
        this.drawOfferedByColor = null;
    }

    private Map<String, Integer> parseOccurrences() {
        Map<String, Integer> values = new LinkedHashMap<>();
        if (positionOccurrences == null || positionOccurrences.isBlank()) return values;
        for (String item : positionOccurrences.split(",")) {
            String[] parts = item.split(":", 2);
            if (parts.length == 2) {
                String key = new String(java.util.Base64.getUrlDecoder().decode(parts[0]), java.nio.charset.StandardCharsets.UTF_8);
                values.put(key, Integer.parseInt(parts[1]));
            }
        }
        return values;
    }

    public ChessFen chessFen() { return ChessFen.parse(fen); }

    @PrePersist void onCreate() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public GameRoom getRoom() { return room; }
    public String getBoardFen() { return fen; }
    public ChessColor getCurrentTurn() { return chessFen().activeColor(); }
    public int getMoveCount() { return moveCount; }
    public long getWhiteRemainingMillis() { return whiteRemainingMillis; }
    public long getBlackRemainingMillis() { return blackRemainingMillis; }
    public Instant getTurnStartedAt() { return turnStartedAt; }
    public ChessColor getDrawOfferedByColor() { return drawOfferedByColor; }
    public int getHalfmoveClock() { return chessFen().halfmoveClock(); }
    public ChessColor getWinnerColor() { return winnerColor; }
    public ChessColor getLoserColor() { return loserColor; }
    public ChessFinishReason getFinishReason() { return finishReason; }
    public Instant getFinishedAt() { return finishedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
