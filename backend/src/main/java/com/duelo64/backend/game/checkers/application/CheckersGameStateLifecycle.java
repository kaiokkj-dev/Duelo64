package com.duelo64.backend.game.checkers.application;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.duelo64.backend.game.checkers.persistence.CheckersGameStateRepository;
import com.duelo64.backend.game.room.GameRoom;
import com.duelo64.backend.game.room.GameStateLifecycle;
import com.duelo64.backend.game.room.GameType;

@Component
public class CheckersGameStateLifecycle implements GameStateLifecycle {
    private final CheckersGameService gameService;
    private final CheckersGameStateRepository stateRepository;

    public CheckersGameStateLifecycle(
            CheckersGameService gameService,
            CheckersGameStateRepository stateRepository) {
        this.gameService = gameService;
        this.stateRepository = stateRepository;
    }

    @Override
    public GameType gameType() {
        return GameType.CHECKERS;
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

    @Override
    public void resolveTimeout(GameRoom room, UUID userId) {
        gameService.confirmTimeout(userId, room.getCode());
    }
}
