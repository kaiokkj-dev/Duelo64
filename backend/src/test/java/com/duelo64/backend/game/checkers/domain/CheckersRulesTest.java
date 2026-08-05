package com.duelo64.backend.game.checkers.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CheckersRulesTest {

    private final CheckersRules rules = new CheckersRules();

    @Test
    void shouldPromoteWhitePieceWhenMoveEndsOnRowZero() {
        CheckersBoard result = rules.applyMove(board(
                "........", "..w.....", "........", "........",
                "........", "........", "........", "........"),
                PieceColor.WHITE, pos(1, 2), pos(0, 1));

        assertThat(result.pieceAt(pos(0, 1)).isKing()).isTrue();
        assertThat(result.toNotation().charAt(1)).isEqualTo('W');
    }

    @Test
    void shouldPromoteBlackPieceWhenMoveEndsOnRowSeven() {
        CheckersBoard result = rules.applyMove(board(
                "........", "........", "........", "........",
                "........", "........", ".b......", "........"),
                PieceColor.BLACK, pos(6, 1), pos(7, 2));

        assertThat(result.pieceAt(pos(7, 2)).isKing()).isTrue();
    }

    @Test
    void shouldNotPromoteManThatOnlyPassesPromotionRowDuringCaptureSequence() {
        CheckersBoard initial = board(
                "........", "..b.b...", ".w......", "........",
                "........", "........", "........", "........");

        CheckersMoveResult first = rules.applyMoveDetailed(
                initial, PieceColor.WHITE, pos(2, 1), pos(0, 3), null);
        assertThat(first.mustContinueCapture()).isTrue();
        assertThat(first.board().pieceAt(pos(0, 3)).isKing()).isFalse();

        CheckersMoveResult second = rules.applyMoveDetailed(
                first.board(), PieceColor.WHITE, pos(0, 3), pos(2, 5), pos(0, 3));
        assertThat(second.mustContinueCapture()).isFalse();
        assertThat(second.board().pieceAt(pos(2, 5)).isKing()).isFalse();
    }

    @Test
    void shouldCaptureForwardWithMan() {
        CheckersMoveResult result = rules.applyMoveDetailed(board(
                "........", "........", "........", "..b.....",
                ".w......", "........", "........", "........"),
                PieceColor.WHITE, pos(4, 1), pos(2, 3), null);

        assertThat(result.capture()).isTrue();
        assertThat(result.board().pieceAt(pos(3, 2))).isNull();
    }

    @Test
    void shouldCaptureBackwardWithMan() {
        CheckersMoveResult result = rules.applyMoveDetailed(board(
                "........", "........", "........", "..w.....",
                "...b....", "........", "........", "........"),
                PieceColor.WHITE, pos(3, 2), pos(5, 4), null);

        assertThat(result.capture()).isTrue();
        assertThat(result.board().pieceAt(pos(4, 3))).isNull();
    }

    @Test
    void shouldRequireTwoStepCaptureSequence() {
        CheckersMoveResult first = rules.applyMoveDetailed(board(
                "........", "........", "...b....", "........",
                ".b......", "w.......", "........", "........"),
                PieceColor.WHITE, pos(5, 0), pos(3, 2), null);

        assertThat(first.mustContinueCapture()).isTrue();
        CheckersMoveResult second = rules.applyMoveDetailed(
                first.board(), PieceColor.WHITE, pos(3, 2), pos(1, 4), pos(3, 2));
        assertThat(second.mustContinueCapture()).isFalse();
        assertThat(second.board().countPieces(PieceColor.BLACK)).isZero();
    }

    @Test
    void shouldRejectAnotherPieceDuringMultipleCapture() {
        CheckersBoard board = board(
                "........", "........", "...b....", "........",
                ".b......", "w...w...", "........", "........");

        CheckersMoveResult first = rules.applyMoveDetailed(
                board, PieceColor.WHITE, pos(5, 0), pos(3, 2), null);

        assertThatThrownBy(() -> rules.applyMoveDetailed(
                first.board(), PieceColor.WHITE, pos(5, 4), pos(4, 3), pos(3, 2)))
                .isInstanceOf(InvalidCheckersMoveException.class)
                .hasMessageContaining("mesma peca");
    }

    @Test
    void shouldBlockSimpleMoveWhenCaptureIsAvailable() {
        CheckersBoard board = board(
                "........", "........", "........", "..b.....",
                ".w......", "........", ".....w..", "........");

        assertThatThrownBy(() -> rules.applyMove(
                board, PieceColor.WHITE, pos(6, 5), pos(5, 4)))
                .isInstanceOf(InvalidCheckersMoveException.class)
                .hasMessageContaining("captura obrigatoria");
    }

    @Test
    void majorityRuleShouldRejectShorterSequence() {
        CheckersBoard board = board(
                "........", "........", "........", "..b.....",
                "........", "..b.b...", "...w....", "........");

        assertThatThrownBy(() -> rules.applyMoveDetailed(
                board, PieceColor.WHITE, pos(6, 3), pos(4, 5), null))
                .isInstanceOf(InvalidCheckersMoveException.class)
                .hasMessageContaining("Maioria");
    }

    @Test
    void kingShouldMoveAcrossSeveralEmptySquares() {
        CheckersBoard result = rules.applyMove(board(
                "........", "........", "........", "........",
                "........", "........", "........", "W......."),
                PieceColor.WHITE, pos(7, 0), pos(3, 4));

        assertThat(result.pieceAt(pos(3, 4)).isKing()).isTrue();
    }

    @Test
    void kingShouldCaptureAndChooseLandingSquare() {
        CheckersMoveResult result = rules.applyMoveDetailed(board(
                "........", "........", "........", "........",
                "...b....", "........", "........", "W......."),
                PieceColor.WHITE, pos(7, 0), pos(2, 5), null);

        assertThat(result.board().pieceAt(pos(4, 3))).isNull();
        assertThat(result.board().pieceAt(pos(2, 5)).isKing()).isTrue();
    }

    @Test
    void kingShouldContinueMultipleCapture() {
        CheckersMoveResult first = rules.applyMoveDetailed(board(
                "........", "........", ".....b..", "........",
                "........", "..b.....", "........", "W......."),
                PieceColor.WHITE, pos(7, 0), pos(4, 3), null);

        assertThat(first.mustContinueCapture()).isTrue();
        CheckersMoveResult second = rules.applyMoveDetailed(
                first.board(), PieceColor.WHITE, pos(4, 3), pos(1, 6), pos(4, 3));
        assertThat(second.mustContinueCapture()).isFalse();
    }

    @Test
    void shouldDetectPlayerWithoutPieces() {
        CheckersBoard board = board(
                "........", "........", "........", "........",
                "........", "........", ".w......", "........");

        assertThat(board.countPieces(PieceColor.BLACK)).isZero();
    }

    @Test
    void shouldDetectPlayerWithoutLegalMoves() {
        CheckersBoard board = board(
                "........", "........", "........", "........",
                "........", "........", "........", "b.......");

        assertThat(board.countPieces(PieceColor.BLACK)).isOne();
        assertThat(rules.hasAnyLegalMove(board, PieceColor.BLACK)).isFalse();
    }

    private CheckersBoard board(String... rows) {
        return CheckersBoard.fromNotation(String.join("", rows));
    }

    private BoardPosition pos(int row, int column) {
        return new BoardPosition(row, column);
    }
}
