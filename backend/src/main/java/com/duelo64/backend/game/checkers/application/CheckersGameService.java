package com.duelo64.backend.game.checkers.application;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.duelo64.backend.game.checkers.api.MovePieceRequest;
import com.duelo64.backend.game.checkers.domain.BoardPosition;
import com.duelo64.backend.game.checkers.domain.CheckersBoard;
import com.duelo64.backend.game.checkers.domain.CheckersMoveResult;
import com.duelo64.backend.game.checkers.domain.CheckersRules;
import com.duelo64.backend.game.checkers.domain.PieceColor;
import com.duelo64.backend.game.checkers.persistence.CheckersGameState;
import com.duelo64.backend.game.checkers.persistence.CheckersGameStateRepository;
import com.duelo64.backend.game.checkers.persistence.CheckersFinishReason;
import com.duelo64.backend.game.room.GameRoom;
import com.duelo64.backend.game.room.GameRoomRepository;
import com.duelo64.backend.game.room.RoomRealtimeEvent;
import com.duelo64.backend.game.room.RoomRealtimePublisher;
import com.duelo64.backend.game.room.RoomNotFoundException;
import com.duelo64.backend.game.room.RoomStatus;

@Service
public class CheckersGameService {

    private final CheckersGameStateRepository checkersGameStateRepository;
    private final GameRoomRepository gameRoomRepository;
    private final RoomRealtimePublisher roomRealtimePublisher;
    private final CheckersRules checkersRules = new CheckersRules();

    public CheckersGameService(
            CheckersGameStateRepository checkersGameStateRepository,
            GameRoomRepository gameRoomRepository,
            RoomRealtimePublisher roomRealtimePublisher) {

        this.checkersGameStateRepository = checkersGameStateRepository;
        this.gameRoomRepository = gameRoomRepository;
        this.roomRealtimePublisher = roomRealtimePublisher;
    }

    @Transactional
    public CheckersGameState createInitialState(GameRoom room) {
        if (checkersGameStateRepository.existsByRoomId(room.getId())) {
            return checkersGameStateRepository
                    .findByRoomCode(room.getCode())
                    .orElseThrow(RoomNotFoundException::new);
        }

        return checkersGameStateRepository.save(CheckersGameState.start(room));
    }

    @Transactional
    public CheckersGameState getState(String roomCode) {
        CheckersGameState state = findState(roomCode);
        finishOnTimeout(state, Instant.now());
        return state;
    }

    @Transactional
    public CheckersGameState movePiece(UUID userId, String roomCode, MovePieceRequest request) {
        CheckersGameState state = findState(roomCode);
        GameRoom room = state.getRoom();

        if (room.getStatus() != RoomStatus.IN_PROGRESS) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A partida ainda nao esta em andamento.");
        }

        PieceColor playerColor = playerColorFor(room, userId);

        Instant now = Instant.now();
        state.consumeCurrentTurnTime(now);

        if (state.hasCurrentPlayerRunOutOfTime()) {
            finishOnTimeout(state, now);
            return state;
        }

        if (playerColor != state.getCurrentTurn()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ainda nao e sua vez.");
        }

        CheckersBoard board = CheckersBoard.fromNotation(state.getBoardNotation());
        BoardPosition forcedPiece = state.mustContinueCapture()
                ? new BoardPosition(state.getForcedCaptureRow(), state.getForcedCaptureColumn())
                : null;
        CheckersMoveResult result = checkersRules.applyMoveDetailed(
                board,
                state.getCurrentTurn(),
                new BoardPosition(request.fromRow(), request.fromColumn()),
                new BoardPosition(request.toRow(), request.toColumn()),
                forcedPiece);

        state.apply(result, now);

        if (!result.mustContinueCapture()) {
            finishIfOpponentCannotPlay(state, playerColor);
        }

        roomRealtimePublisher.publish(RoomRealtimeEvent.gameStateUpdated(room));

        return state;
    }

    private void finishOnTimeout(CheckersGameState state, Instant now) {
        GameRoom room = state.getRoom();

        if (room.getStatus() != RoomStatus.IN_PROGRESS) {
            return;
        }

        state.consumeCurrentTurnTime(now);

        if (!state.hasCurrentPlayerRunOutOfTime()) {
            return;
        }

        room.finish();
        state.finish(state.getCurrentTurn().opponent(), CheckersFinishReason.TIMEOUT);
        roomRealtimePublisher.publish(RoomRealtimeEvent.gameStateUpdated(room));
    }

    void finishIfOpponentCannotPlay(CheckersGameState state, PieceColor moverColor) {
        CheckersBoard board = CheckersBoard.fromNotation(state.getBoardNotation());
        PieceColor opponent = moverColor.opponent();
        CheckersFinishReason reason = null;

        if (board.countPieces(opponent) == 0) {
            reason = CheckersFinishReason.NO_PIECES;
        } else if (!checkersRules.hasAnyLegalMove(board, opponent)) {
            reason = CheckersFinishReason.NO_LEGAL_MOVES;
        }

        if (reason == null) {
            return;
        }

        state.finish(moverColor, reason);
        state.getRoom().finish();
    }

    private CheckersGameState findState(String roomCode) {
        return checkersGameStateRepository
                .findByRoomCode(normalizeCode(roomCode))
                .orElseGet(() -> createStateForExistingRoom(roomCode));
    }

    private CheckersGameState createStateForExistingRoom(String roomCode) {
        GameRoom room = gameRoomRepository
                .findByCode(normalizeCode(roomCode))
                .orElseThrow(RoomNotFoundException::new);

        return checkersGameStateRepository.save(CheckersGameState.start(room));
    }

    private PieceColor playerColorFor(GameRoom room, UUID userId) {
        if (room.getHost().getId().equals(userId)) {
            return PieceColor.WHITE;
        }

        if (room.getGuest() != null && room.getGuest().getId().equals(userId)) {
            return PieceColor.BLACK;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Voce nao participa desta partida.");
    }

    private String normalizeCode(String code) {
        if (code == null) {
            return "";
        }

        return code.trim().toUpperCase(Locale.ROOT);
    }
}
