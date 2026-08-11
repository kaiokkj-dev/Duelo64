package com.duelo64.backend.game.chess.application;

import org.springframework.stereotype.Component;

import com.duelo64.backend.game.room.*;
import com.duelo64.backend.game.chess.persistence.ChessGameStateRepository;

@Component
public class ChessGameStateLifecycle implements GameStateLifecycle {
    private final ChessGameService gameService;
    private final ChessGameStateRepository stateRepository;

    public ChessGameStateLifecycle(ChessGameService gameService, ChessGameStateRepository stateRepository) {
        this.gameService = gameService;
        this.stateRepository = stateRepository;
    }

    @Override
    public GameType gameType() {
        return GameType.CHESS;
    }

    @Override
    public void initialize(GameRoom room) {
        gameService.createInitialState(room);
    }

    @Override
    public void start(GameRoom room) {
        stateRepository.findByRoomId(room.getId())
                .ifPresent(state -> state.startClock(room.getStartedAt()));
    }
}
