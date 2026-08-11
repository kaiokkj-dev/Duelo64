package com.duelo64.backend.game.chess.api;

import java.util.List;

public record ChessLegalMovesResponse(List<ChessLegalMoveResponse> moves) {}
