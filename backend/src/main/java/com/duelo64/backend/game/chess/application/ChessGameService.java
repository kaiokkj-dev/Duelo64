package com.duelo64.backend.game.chess.application;

import java.util.Locale;
import java.util.UUID;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.duelo64.backend.game.chess.domain.*;
import com.duelo64.backend.game.chess.persistence.ChessGameState;
import com.duelo64.backend.game.chess.persistence.ChessGameStateRepository;
import com.duelo64.backend.game.chess.persistence.ChessFinishReason;
import com.duelo64.backend.game.room.*;
import com.duelo64.backend.game.match.*;

@Service
public class ChessGameService {
    private final ChessGameStateRepository stateRepository;
    private final GameRoomRepository roomRepository;
    private final RoomRealtimePublisher realtimePublisher;
    private final MatchHistoryService matchHistoryService;
    private final ChessRules rules;

    public ChessGameService(
            ChessGameStateRepository stateRepository,
            GameRoomRepository roomRepository,
            RoomRealtimePublisher realtimePublisher,
            MatchHistoryService matchHistoryService) {
        this.stateRepository = stateRepository;
        this.roomRepository = roomRepository;
        this.realtimePublisher = realtimePublisher;
        this.matchHistoryService = matchHistoryService;
        this.rules = new ChessRules();
    }

    @Transactional
    public ChessGameState createInitialState(GameRoom room) {
        requireChessRoom(room);
        return stateRepository.findByRoomId(room.getId())
                .orElseGet(() -> stateRepository.save(ChessGameState.start(room)));
    }

    @Transactional
    public ChessGameState getState(UUID userId, String roomCode) {
        ChessGameState state = findState(roomCode);
        playerColorFor(state.getRoom(), userId);
        finishOnTimeout(state, Instant.now());
        return state;
    }

    @Transactional
    public ChessGameState move(
            UUID userId,
            String roomCode,
            ChessPosition from,
            ChessPosition to) {
        return move(userId, roomCode, from, to, null);
    }

    @Transactional
    public ChessGameState move(
            UUID userId,
            String roomCode,
            ChessPosition from,
            ChessPosition to,
            ChessPieceType promotion) {
        ChessGameState state = findState(roomCode);
        if (state.getRoom().getStatus() != RoomStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A partida nao esta em andamento.");
        }
        ChessColor playerColor = playerColorFor(state.getRoom(), userId);
        if (playerColor != state.getCurrentTurn()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ainda nao e sua vez.");
        }

        Instant now = Instant.now();
        state.consumeCurrentTurnTime(now);
        if (state.hasCurrentPlayerRunOutOfTime()) {
            complete(state, state.getCurrentTurn().opponent(), ChessFinishReason.TIMEOUT);
            publish(state);
            return state;
        }
        ChessFen fen = state.chessFen();
        ChessMoveResult result = rules.applyMove(fen, from, to, promotion);
        state.apply(result, now);
        finishIfRequired(state);
        ChessGameState saved = stateRepository.save(state);
        realtimePublisher.publish(RoomRealtimeEvent.gameStateUpdated(state.getRoom()));
        return saved;
    }

    @Transactional
    public java.util.List<ChessLegalMove> legalMoves(UUID userId, String roomCode, ChessPosition from) {
        ChessGameState state = findState(roomCode);
        finishOnTimeout(state, Instant.now());
        if (state.getRoom().getStatus() != RoomStatus.IN_PROGRESS) return java.util.List.of();
        ChessColor color = playerColorFor(state.getRoom(), userId);
        if (color != state.getCurrentTurn()) return java.util.List.of();
        return rules.legalMoves(state.chessFen(), from);
    }

    @Transactional
    public ChessGameState resign(UUID userId, String roomCode) {
        ChessGameState state = findInProgressState(roomCode);
        ChessColor color = playerColorFor(state.getRoom(), userId);
        complete(state, color.opponent(), ChessFinishReason.RESIGNATION);
        publish(state);
        return state;
    }

    @Transactional
    public ChessGameState confirmTimeout(UUID userId, String roomCode) {
        ChessGameState state = findState(roomCode);
        playerColorFor(state.getRoom(), userId);
        finishOnTimeout(state, Instant.now());
        return state;
    }

    @Transactional
    public ChessGameState offerDraw(UUID userId, String roomCode) {
        ChessGameState state = findInProgressState(roomCode);
        ChessColor color = playerColorFor(state.getRoom(), userId);
        if (state.hasPendingDrawOffer()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Ja existe uma oferta de empate pendente.");
        state.offerDraw(color);
        publish(state);
        return state;
    }

    @Transactional
    public ChessGameState acceptDraw(UUID userId, String roomCode) {
        ChessGameState state = findInProgressState(roomCode);
        validateDrawResponse(state, playerColorFor(state.getRoom(), userId));
        complete(state, null, ChessFinishReason.DRAW_AGREEMENT);
        publish(state);
        return state;
    }

    @Transactional
    public ChessGameState declineDraw(UUID userId, String roomCode) {
        ChessGameState state = findInProgressState(roomCode);
        validateDrawResponse(state, playerColorFor(state.getRoom(), userId));
        state.declineDraw();
        publish(state);
        return state;
    }

    void finishIfRequired(ChessGameState state) {
        if (state.getRoom().getStatus() == RoomStatus.FINISHED) return;
        ChessFen fen = state.chessFen();
        ChessColor playerToMove = fen.activeColor();
        ChessFinishReason reason = null;
        ChessColor winner = null;

        if (!rules.hasAnyLegalMove(fen)) {
            if (rules.isInCheck(fen.board(), playerToMove)) {
                reason = ChessFinishReason.CHECKMATE;
                winner = playerToMove.opponent();
            } else {
                reason = ChessFinishReason.STALEMATE;
            }
        } else if (fen.halfmoveClock() >= 100) {
            reason = ChessFinishReason.DRAW_FIFTY_MOVE_RULE;
        } else {
            boolean enPassantRelevant = rules.hasLegalEnPassant(fen);
            if (state.recordCurrentPosition(enPassantRelevant) >= 3) {
                reason = ChessFinishReason.DRAW_REPETITION;
            } else if (rules.isInsufficientMaterial(fen.board())) {
                reason = ChessFinishReason.DRAW_INSUFFICIENT_MATERIAL;
            }
        }

        if (reason != null) {
            complete(state, winner, reason);
        }
    }

    private void finishOnTimeout(ChessGameState state, Instant now) {
        if (state.getRoom().getStatus() != RoomStatus.IN_PROGRESS) return;
        state.consumeCurrentTurnTime(now);
        if (!state.hasCurrentPlayerRunOutOfTime()) return;
        complete(state, state.getCurrentTurn().opponent(), ChessFinishReason.TIMEOUT);
        publish(state);
    }

    private void complete(ChessGameState state, ChessColor winner, ChessFinishReason reason) {
        if (state.getRoom().getStatus() == RoomStatus.FINISHED) return;
        state.finish(winner, reason, Instant.now());
        state.getRoom().finish();
        MatchResult result = winner == null ? MatchResult.DRAW
                : winner == ChessColor.WHITE ? MatchResult.WHITE_WIN : MatchResult.BLACK_WIN;
        matchHistoryService.recordCompletedMatch(new CompletedMatchSnapshot(
                state.getRoom(), result, reason.name(), state.getMoveCount()));
    }

    private ChessGameState findInProgressState(String roomCode) {
        ChessGameState state = findState(roomCode);
        if (state.getRoom().getStatus() != RoomStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A partida nao esta em andamento.");
        }
        return state;
    }

    private void validateDrawResponse(ChessGameState state, ChessColor color) {
        if (!state.hasPendingDrawOffer()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Nao existe oferta de empate pendente.");
        if (state.getDrawOfferedByColor() == color) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Voce nao pode responder a propria oferta.");
    }

    private void publish(ChessGameState state) {
        realtimePublisher.publish(RoomRealtimeEvent.gameStateUpdated(state.getRoom()));
    }

    private ChessGameState findState(String roomCode) {
        String code = roomCode == null ? "" : roomCode.trim().toUpperCase(Locale.ROOT);
        return stateRepository.findByRoomCode(code).orElseGet(() -> {
            GameRoom room = roomRepository.findByCode(code).orElseThrow(RoomNotFoundException::new);
            return createInitialState(room);
        });
    }

    private ChessColor playerColorFor(GameRoom room, UUID userId) {
        if (room.getHost().getId().equals(userId)) return ChessColor.WHITE;
        if (room.getGuest() != null && room.getGuest().getId().equals(userId)) return ChessColor.BLACK;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Voce nao participa desta partida.");
    }

    private void requireChessRoom(GameRoom room) {
        if (room.getGameType() != GameType.CHESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta sala nao pertence ao Xadrez.");
        }
    }
}
