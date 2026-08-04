package com.duelo64.backend.game.checkers.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CheckersRulesTest {

    private final CheckersRules rules = new CheckersRules();

    @Test
    void shouldMoveWhitePieceForward() {
        CheckersBoard board = CheckersBoard.initial();

        CheckersBoard nextBoard = rules.applyMove(
                board,
                PieceColor.WHITE,
                new BoardPosition(5, 0),
                new BoardPosition(4, 1));

        assertThat(nextBoard.pieceAt(new BoardPosition(5, 0))).isNull();
        assertThat(nextBoard.pieceAt(new BoardPosition(4, 1)).getColor()).isEqualTo(PieceColor.WHITE);
    }

    @Test
    void shouldCaptureOpponentPiece() {
        CheckersBoard board = CheckersBoard.fromNotation("""
                ........
                ........
                ........
                ..b.....
                .w......
                ........
                ........
                ........
                """.replace("\n", ""));

        CheckersBoard nextBoard = rules.applyMove(
                board,
                PieceColor.WHITE,
                new BoardPosition(4, 1),
                new BoardPosition(2, 3));

        assertThat(nextBoard.pieceAt(new BoardPosition(4, 1))).isNull();
        assertThat(nextBoard.pieceAt(new BoardPosition(3, 2))).isNull();
        assertThat(nextBoard.pieceAt(new BoardPosition(2, 3)).getColor()).isEqualTo(PieceColor.WHITE);
    }

    @Test
    void shouldBlockSimpleMoveWhenCaptureIsAvailable() {
        CheckersBoard board = CheckersBoard.fromNotation("""
                ........
                ........
                ........
                ..b.....
                .w......
                ........
                .....w..
                ........
                """.replace("\n", ""));

        assertThatThrownBy(() -> rules.applyMove(
                board,
                PieceColor.WHITE,
                new BoardPosition(6, 5),
                new BoardPosition(5, 4)))
                .isInstanceOf(InvalidCheckersMoveException.class)
                .hasMessageContaining("captura obrigatoria");
    }
}
