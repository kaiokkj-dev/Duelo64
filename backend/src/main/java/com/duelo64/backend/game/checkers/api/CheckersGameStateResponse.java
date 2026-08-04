package com.duelo64.backend.game.checkers.api;

import java.util.List;

import com.duelo64.backend.game.checkers.domain.CheckersBoard;
import com.duelo64.backend.game.checkers.persistence.CheckersGameState;

public record CheckersGameStateResponse(
        String roomCode,
        List<List<String>> board,
        String currentTurn,
        int moveCount) {

    public static CheckersGameStateResponse from(CheckersGameState state) {
        CheckersBoard board = CheckersBoard.fromNotation(state.getBoardNotation());

        return new CheckersGameStateResponse(
                state.getRoom().getCode(),
                board.toMatrix(),
                state.getCurrentTurn().name(),
                state.getMoveCount());
    }
}
