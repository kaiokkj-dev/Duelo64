package com.duelo64.backend.game.checkers.application;

import java.time.Instant;
import java.util.Locale;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.duelo64.backend.game.checkers.api.MovePieceRequest;
import com.duelo64.backend.game.checkers.domain.BoardPosition;
import com.duelo64.backend.game.checkers.domain.CheckersBoard;
import com.duelo64.backend.game.checkers.domain.CheckersMoveResult;
import com.duelo64.backend.game.checkers.domain.CheckersLegalMove;
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
import com.duelo64.backend.game.room.GameType;
import com.duelo64.backend.game.match.MatchHistoryService;
import com.duelo64.backend.game.match.CompletedMatchSnapshot;
import com.duelo64.backend.game.match.MatchResult;

@Service
public class CheckersGameService {

    private final CheckersGameStateRepository checkersGameStateRepository;
    private final GameRoomRepository gameRoomRepository;
    private final RoomRealtimePublisher roomRealtimePublisher;
    private final MatchHistoryService matchHistoryService;
    private final CheckersRules checkersRules = new CheckersRules();

    public CheckersGameService(
            CheckersGameStateRepository checkersGameStateRepository,
            GameRoomRepository gameRoomRepository,
            RoomRealtimePublisher roomRealtimePublisher,
            MatchHistoryService matchHistoryService) {

        this.checkersGameStateRepository = checkersGameStateRepository;
        this.gameRoomRepository = gameRoomRepository;
        this.roomRealtimePublisher = roomRealtimePublisher;
        this.matchHistoryService = matchHistoryService;
    }

    @Transactional
    public CheckersGameState createInitialState(GameRoom room) {
        requireCheckersRoom(room);
        if (checkersGameStateRepository.existsByRoomId(room.getId())) {
            return checkersGameStateRepository
                    .findByRoomCode(room.getCode())
                    .orElseThrow(RoomNotFoundException::new);
        }

        return checkersGameStateRepository.save(CheckersGameState.start(room));
    }

    @Transactional
    public CheckersGameState getState(UUID userId, String roomCode) {
        CheckersGameState state = findState(roomCode);
        playerColorFor(state.getRoom(), userId);
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

        boolean movedPieceWasKing = board.pieceAt(
                new BoardPosition(request.fromRow(), request.fromColumn())).isKing();
        state.apply(result, now);
        state.updateAutomaticDrawCounters(result, movedPieceWasKing);

        if (!result.mustContinueCapture()) {
            finishIfOpponentCannotPlay(state, playerColor);
            if (room.getStatus() == RoomStatus.IN_PROGRESS) {
                finishIfAutomaticDraw(state);
            }
        }

        roomRealtimePublisher.publish(RoomRealtimeEvent.gameStateUpdated(room));

        return state;
    }

    @Transactional
    public CheckersGameState confirmTimeout(UUID userId, String roomCode) {
        CheckersGameState state = findState(roomCode);
        GameRoom room = state.getRoom();

        playerColorFor(room, userId);
        finishOnTimeout(state, Instant.now());
        return state;
    }

    @Transactional
    public CheckersGameState resign(UUID userId, String roomCode) {
        CheckersGameState state = findInProgressState(roomCode);
        PieceColor resigningColor = playerColorFor(state.getRoom(), userId);

        completeMatch(state, resigningColor.opponent(), CheckersFinishReason.RESIGNATION);
        publishStateUpdated(state);
        return state;
    }

    @Transactional
    public CheckersGameState offerDraw(UUID userId, String roomCode) {
        CheckersGameState state = findInProgressState(roomCode);
        PieceColor offeredByColor = playerColorFor(state.getRoom(), userId);

        if (state.hasPendingDrawOffer()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ja existe uma oferta de empate pendente.");
        }

        state.offerDraw(offeredByColor);
        publishStateUpdated(state);
        return state;
    }

    @Transactional
    public CheckersGameState acceptDraw(UUID userId, String roomCode) {
        CheckersGameState state = findInProgressState(roomCode);
        PieceColor playerColor = playerColorFor(state.getRoom(), userId);
        validateDrawResponse(state, playerColor);

        completeMatch(state, null, CheckersFinishReason.DRAW_AGREEMENT);
        publishStateUpdated(state);
        return state;
    }

    @Transactional
    public CheckersGameState declineDraw(UUID userId, String roomCode) {
        CheckersGameState state = findInProgressState(roomCode);
        PieceColor playerColor = playerColorFor(state.getRoom(), userId);
        validateDrawResponse(state, playerColor);

        state.declineDraw();
        publishStateUpdated(state);
        return state;
    }

    @Transactional
    public List<CheckersLegalMove> legalMoves(
            UUID userId,
            String roomCode,
            int row,
            int column) {

        CheckersGameState state = findState(roomCode);
        finishOnTimeout(state, Instant.now());
        GameRoom room = state.getRoom();

        if (room.getStatus() != RoomStatus.IN_PROGRESS) {
            return List.of();
        }

        PieceColor playerColor = playerColorFor(room, userId);
        if (playerColor != state.getCurrentTurn()) {
            return List.of();
        }

        BoardPosition from = new BoardPosition(row, column);
        if (!from.isInsideBoard()) {
            return List.of();
        }

        CheckersBoard board = CheckersBoard.fromNotation(state.getBoardNotation());
        if (board.pieceAt(from) == null || board.pieceAt(from).getColor() != playerColor) {
            return List.of();
        }

        BoardPosition forcedPiece = state.mustContinueCapture()
                ? new BoardPosition(state.getForcedCaptureRow(), state.getForcedCaptureColumn())
                : null;

        if (forcedPiece != null && !forcedPiece.equals(from)) {
            return List.of();
        }

        return checkersRules.legalMoves(board, state.getCurrentTurn(), from, forcedPiece);
    }

    private void finishOnTimeout(CheckersGameState state, Instant now) {
        GameRoom room = state.getRoom();

        if (room.getStatus() != RoomStatus.IN_PROGRESS) {
            return;
        }

        long remainingMillis = state.getCurrentTurn() == PieceColor.WHITE
                ? state.calculateWhiteRemainingMillis(now)
                : state.calculateBlackRemainingMillis(now);

        if (remainingMillis > 0) {
            return;
        }

        state.consumeCurrentTurnTime(now);

        completeMatch(state, state.getCurrentTurn().opponent(), CheckersFinishReason.TIMEOUT);
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

        completeMatch(state, moverColor, reason);
    }

    void finishIfAutomaticDraw(CheckersGameState state) {
        CheckersFinishReason reason = null;

        if (state.recordCurrentPosition() >= 3) {
            reason = CheckersFinishReason.DRAW_REPETITION;
        } else if (state.getKingOnlyMoveCount() >= 40) {
            reason = CheckersFinishReason.DRAW_MOVE_LIMIT;
        }

        if (reason == null) return;
        completeMatch(state, null, reason);
    }

    private void completeMatch(CheckersGameState state, PieceColor winner, CheckersFinishReason reason) {
        if (state.getRoom().getStatus() == RoomStatus.FINISHED) return;
        state.finish(winner, reason);
        state.getRoom().finish();
        MatchResult result = winner == null ? MatchResult.DRAW
                : winner == PieceColor.WHITE ? MatchResult.WHITE_WIN : MatchResult.BLACK_WIN;
        matchHistoryService.recordCompletedMatch(new CompletedMatchSnapshot(
                state.getRoom(), result, reason.name(), state.getMoveCount()));
    }

    private CheckersGameState findState(String roomCode) {
        return checkersGameStateRepository
                .findByRoomCode(normalizeCode(roomCode))
                .orElseGet(() -> createStateForExistingRoom(roomCode));
    }

    private CheckersGameState findInProgressState(String roomCode) {
        CheckersGameState state = findState(roomCode);
        if (state.getRoom().getStatus() != RoomStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A partida nao esta em andamento.");
        }
        return state;
    }

    private void validateDrawResponse(CheckersGameState state, PieceColor playerColor) {
        if (!state.hasPendingDrawOffer()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nao existe oferta de empate pendente.");
        }
        if (state.getDrawOfferedByColor() == playerColor) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Voce nao pode responder a propria oferta.");
        }
    }

    private void publishStateUpdated(CheckersGameState state) {
        roomRealtimePublisher.publish(RoomRealtimeEvent.gameStateUpdated(state.getRoom()));
    }

    private CheckersGameState createStateForExistingRoom(String roomCode) {
        GameRoom room = gameRoomRepository
                .findByCode(normalizeCode(roomCode))
                .orElseThrow(RoomNotFoundException::new);

        requireCheckersRoom(room);

        return checkersGameStateRepository.save(CheckersGameState.start(room));
    }

    private void requireCheckersRoom(GameRoom room) {
        if (room.getGameType() != GameType.CHECKERS) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Esta sala nao pertence a modalidade Damas.");
        }
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
