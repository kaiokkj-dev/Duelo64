package com.duelo64.backend.game.match;

import com.duelo64.backend.game.room.GameRoom;

/** Game-neutral data needed to persist a finished match. */
public record CompletedMatchSnapshot(
        GameRoom room,
        MatchResult result,
        String finishReason,
        int moveCount) {
}
