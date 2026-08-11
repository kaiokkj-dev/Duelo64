package com.duelo64.backend.game.chess.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ChessRulesTest {
    private final ChessRules rules = new ChessRules();

    @Test
    void pawnMovesOneOrTwoOnlyFromInitialRowAndNeverBackwards() {
        ChessBoard initial = ChessBoard.initial();
        assertThat(move(initial, ChessColor.WHITE, 6, 4, 5, 4).pieceAt(p(5, 4)).type())
                .isEqualTo(ChessPieceType.PAWN);
        assertThat(move(initial, ChessColor.WHITE, 6, 4, 4, 4).pieceAt(p(4, 4)).type())
                .isEqualTo(ChessPieceType.PAWN);

        ChessBoard advanced = board("4k3/8/8/8/8/4P3/8/4K3");
        invalid(() -> move(advanced, ChessColor.WHITE, 5, 4, 3, 4));
        invalid(() -> move(advanced, ChessColor.WHITE, 5, 4, 6, 4));
    }

    @Test
    void pawnCapturesOnlyDiagonally() {
        ChessMoveResult result = rules.applyMove(
                board("4k3/8/8/3p4/4P3/8/8/4K3"), ChessColor.WHITE, p(4, 4), p(3, 3));
        assertThat(result.capture()).isTrue();
        assertThat(result.board().pieceAt(p(3, 3)).color()).isEqualTo(ChessColor.WHITE);
        invalid(() -> move(board("4k3/8/8/8/4P3/8/8/4K3"), ChessColor.WHITE, 4, 4, 3, 3));
    }

    @Test
    void knightJumpsOverPieces() {
        ChessBoard moved = move(ChessBoard.initial(), ChessColor.WHITE, 7, 6, 5, 5);
        assertThat(moved.pieceAt(p(5, 5)).type()).isEqualTo(ChessPieceType.KNIGHT);
    }

    @Test
    void bishopRookAndQueenRespectGeometryAndBlocking() {
        invalid(() -> move(ChessBoard.initial(), ChessColor.WHITE, 7, 2, 3, 6));
        invalid(() -> move(ChessBoard.initial(), ChessColor.WHITE, 7, 0, 4, 0));

        ChessBoard bishop = move(board("4k3/8/8/8/8/8/8/2B1K3"), ChessColor.WHITE, 7, 2, 4, 5);
        assertThat(bishop.pieceAt(p(4, 5)).type()).isEqualTo(ChessPieceType.BISHOP);
        ChessBoard rook = move(board("4k3/8/8/8/8/8/8/R3K3"), ChessColor.WHITE, 7, 0, 3, 0);
        assertThat(rook.pieceAt(p(3, 0)).type()).isEqualTo(ChessPieceType.ROOK);

        ChessBoard queenBoard = board("4k3/8/8/8/3Q4/8/8/4K3");
        assertThat(move(queenBoard, ChessColor.WHITE, 4, 3, 4, 7).pieceAt(p(4, 7)).type())
                .isEqualTo(ChessPieceType.QUEEN);
        assertThat(move(queenBoard, ChessColor.WHITE, 4, 3, 1, 6).pieceAt(p(1, 6)).type())
                .isEqualTo(ChessPieceType.QUEEN);
    }

    @Test
    void kingMovesOnlyOneSquareAndCannotMoveIntoCheck() {
        ChessBoard board = board("4k3/8/8/8/8/8/r3r3/4K3");
        invalid(() -> move(board, ChessColor.WHITE, 7, 4, 5, 4));
        invalid(() -> move(board, ChessColor.WHITE, 7, 4, 6, 4));
        assertThat(move(board, ChessColor.WHITE, 7, 4, 7, 3).pieceAt(p(7, 3)).type())
                .isEqualTo(ChessPieceType.KING);
    }

    @Test
    void rejectsOpponentPieceWrongTurnAndOwnDestination() {
        invalid(() -> move(ChessBoard.initial(), ChessColor.WHITE, 1, 0, 2, 0));
        invalid(() -> move(ChessBoard.initial(), ChessColor.BLACK, 6, 0, 5, 0));
        invalid(() -> move(ChessBoard.initial(), ChessColor.WHITE, 7, 1, 6, 3));
    }

    @Test
    void captureRemovesOpponentPiece() {
        ChessMoveResult result = rules.applyMove(
                board("4k3/8/8/8/3r4/8/3R4/4K3"), ChessColor.WHITE, p(6, 3), p(4, 3));
        assertThat(result.capture()).isTrue();
        assertThat(result.board().pieceAt(p(4, 3)))
                .isEqualTo(new ChessPiece(ChessColor.WHITE, ChessPieceType.ROOK));
        assertThat(result.board().pieceAt(p(6, 3))).isNull();
    }

    @Test
    void cannotExposeOwnKingToCheck() {
        ChessBoard board = board("4r2k/8/8/8/8/8/4R3/4K3");
        invalid(() -> move(board, ChessColor.WHITE, 6, 4, 6, 5));
    }

    @Test
    void canBlockCheckOrCaptureCheckingPiece() {
        ChessBoard blocked = move(
                board("4r2k/8/8/8/8/2B5/8/4K3"), ChessColor.WHITE, 5, 2, 3, 4);
        assertThat(rules.isInCheck(blocked, ChessColor.WHITE)).isFalse();

        ChessBoard captured = move(
                board("R3r2k/8/8/8/8/8/8/4K3"), ChessColor.WHITE, 0, 0, 0, 4);
        assertThat(rules.isInCheck(captured, ChessColor.WHITE)).isFalse();
        assertThat(captured.pieceAt(p(0, 4)).type()).isEqualTo(ChessPieceType.ROOK);
    }

    private ChessBoard move(ChessBoard board, ChessColor turn, int fr, int fc, int tr, int tc) {
        return rules.applyMove(board, turn, p(fr, fc), p(tr, tc)).board();
    }

    private ChessBoard board(String fen) { return ChessBoard.fromFenPlacement(fen); }
    private ChessPosition p(int row, int column) { return new ChessPosition(row, column); }
    private void invalid(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOf(InvalidChessMoveException.class);
    }
}

