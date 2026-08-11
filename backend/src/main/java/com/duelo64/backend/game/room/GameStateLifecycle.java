package com.duelo64.backend.game.room;

import java.util.UUID;

/** Connects a game-specific persisted state to the shared room lifecycle. */
public interface GameStateLifecycle {
    GameType gameType();

    void initialize(GameRoom room);

    void start(GameRoom room);

    default void resolveTimeout(GameRoom room, UUID userId) {
    }
}
