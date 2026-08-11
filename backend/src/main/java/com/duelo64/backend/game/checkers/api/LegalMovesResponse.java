package com.duelo64.backend.game.checkers.api;

import java.util.List;

public record LegalMovesResponse(List<LegalMoveResponse> moves) {
}
