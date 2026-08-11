package com.duelo64.backend.game.match;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.duelo64.backend.game.room.GameRoom;
import com.duelo64.backend.game.room.GameType;
import com.duelo64.backend.game.room.MatchType;
import com.duelo64.backend.user.User;

import jakarta.persistence.*;

@Entity
@Table(name = "game_matches", uniqueConstraints = @UniqueConstraint(name = "uk_game_matches_room_id", columnNames = "room_id"))
public class GameMatch {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "room_id", nullable = false)
    private GameRoom room;
    @Enumerated(EnumType.STRING) @Column(name = "game_type", nullable = false, length = 32)
    private GameType gameType;
    @Enumerated(EnumType.STRING) @Column(name = "match_type", nullable = false, length = 16)
    private MatchType matchType;
    @Column(name = "room_code", nullable = false, length = 6)
    private String roomCode;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "white_player_id", nullable = false)
    private User whitePlayer;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "black_player_id", nullable = false)
    private User blackPlayer;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "winner_id")
    private User winner;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "loser_id")
    private User loser;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16)
    private MatchResult result;
    @Column(name = "finish_reason", nullable = false, length = 32)
    private String finishReason;
    @Column(name = "time_control_minutes", nullable = false)
    private int timeControlMinutes;
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;
    @Column(name = "finished_at", nullable = false)
    private Instant finishedAt;
    @Column(name = "duration_millis", nullable = false)
    private long durationMillis;
    @Column(name = "move_count", nullable = false)
    private int moveCount;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "elo_processed", nullable = false)
    private boolean eloProcessed;
    @Column(name = "white_rating_before")
    private Integer whiteRatingBefore;
    @Column(name = "white_rating_after")
    private Integer whiteRatingAfter;
    @Column(name = "black_rating_before")
    private Integer blackRatingBefore;
    @Column(name = "black_rating_after")
    private Integer blackRatingAfter;

    protected GameMatch() {}

    public static GameMatch from(CompletedMatchSnapshot snapshot) {
        GameRoom room = snapshot.room();
        GameMatch match = new GameMatch();
        match.room = room;
        match.gameType = room.getGameType();
        match.matchType = room.getMatchType();
        match.roomCode = room.getCode();
        match.whitePlayer = room.getHost();
        match.blackPlayer = room.getGuest();
        match.finishReason = snapshot.finishReason();
        match.timeControlMinutes = room.getTimeControlMinutes();
        match.startedAt = room.getStartedAt();
        match.finishedAt = room.getFinishedAt();
        match.durationMillis = Math.max(0, Duration.between(match.startedAt, match.finishedAt).toMillis());
        match.moveCount = snapshot.moveCount();
        match.createdAt = Instant.now();

        match.result = snapshot.result();
        if (match.result == MatchResult.WHITE_WIN) {
            match.winner = match.whitePlayer;
            match.loser = match.blackPlayer;
        } else if (match.result == MatchResult.BLACK_WIN) {
            match.winner = match.blackPlayer;
            match.loser = match.whitePlayer;
        }
        return match;
    }

    public UUID getId() { return id; }
    public GameRoom getRoom() { return room; }
    public GameType getGameType() { return gameType; }
    public MatchType getMatchType() { return matchType; }
    public String getRoomCode() { return roomCode; }
    public User getWhitePlayer() { return whitePlayer; }
    public User getBlackPlayer() { return blackPlayer; }
    public User getWinner() { return winner; }
    public User getLoser() { return loser; }
    public MatchResult getResult() { return result; }
    public String getFinishReason() { return finishReason; }
    public int getTimeControlMinutes() { return timeControlMinutes; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public long getDurationMillis() { return durationMillis; }
    public int getMoveCount() { return moveCount; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isEloProcessed() { return eloProcessed; }
    public void markEloProcessed() { this.eloProcessed = true; }
    public void recordEloChange(int whiteBefore, int whiteAfter, int blackBefore, int blackAfter) {
        this.whiteRatingBefore = whiteBefore;
        this.whiteRatingAfter = whiteAfter;
        this.blackRatingBefore = blackBefore;
        this.blackRatingAfter = blackAfter;
    }
    public Integer getWhiteRatingBefore() { return whiteRatingBefore; }
    public Integer getWhiteRatingAfter() { return whiteRatingAfter; }
    public Integer getBlackRatingBefore() { return blackRatingBefore; }
    public Integer getBlackRatingAfter() { return blackRatingAfter; }
}
