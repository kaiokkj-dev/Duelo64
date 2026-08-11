package com.duelo64.backend.game.chess.domain;

public record ChessLegalMove(int toRow, int toColumn, boolean capture, boolean promotionRequired) {}
