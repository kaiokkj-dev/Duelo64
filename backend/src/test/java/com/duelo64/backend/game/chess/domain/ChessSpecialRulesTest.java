package com.duelo64.backend.game.chess.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ChessSpecialRulesTest {
    private final ChessRules rules = new ChessRules();

    @Test
    void promotesToEveryAllowedPieceAndRejectsInvalidPromotion() {
        for (ChessPieceType type : new ChessPieceType[] {
                ChessPieceType.QUEEN, ChessPieceType.ROOK,
                ChessPieceType.BISHOP, ChessPieceType.KNIGHT}) {
            ChessMoveResult result = rules.applyMove(
                    fen("4k3/P7/8/8/8/8/8/4K3 w - - 0 1"), p(1, 0), p(0, 0), type);
            assertThat(result.board().pieceAt(p(0, 0)).type()).isEqualTo(type);
        }
        invalid(() -> rules.applyMove(fen("4k3/P7/8/8/8/8/8/4K3 w - - 0 1"), p(1, 0), p(0, 0), null));
        invalid(() -> rules.applyMove(fen("4k3/P7/8/8/8/8/8/4K3 w - - 0 1"), p(1, 0), p(0, 0), ChessPieceType.KING));
        invalid(() -> rules.applyMove(fen("4k3/P7/8/8/8/8/8/4K3 w - - 0 1"), p(1, 0), p(0, 0), ChessPieceType.PAWN));

        ChessMoveResult black = rules.applyMove(
                fen("4k3/8/8/8/8/8/7p/4K3 b - - 0 1"), p(6, 7), p(7, 7), ChessPieceType.QUEEN);
        assertThat(black.board().pieceAt(p(7, 7)))
                .isEqualTo(new ChessPiece(ChessColor.BLACK, ChessPieceType.QUEEN));
    }

    @Test
    void castlesBothSidesAndMovesRook() {
        ChessFen position = fen("4k3/8/8/8/8/8/8/R3K2R w KQ - 0 1");
        ChessMoveResult kingSide = rules.applyMove(position, p(7, 4), p(7, 6), null);
        assertThat(kingSide.board().pieceAt(p(7, 6)).type()).isEqualTo(ChessPieceType.KING);
        assertThat(kingSide.board().pieceAt(p(7, 5)).type()).isEqualTo(ChessPieceType.ROOK);
        ChessMoveResult queenSide = rules.applyMove(position, p(7, 4), p(7, 2), null);
        assertThat(queenSide.board().pieceAt(p(7, 2)).type()).isEqualTo(ChessPieceType.KING);
        assertThat(queenSide.board().pieceAt(p(7, 3)).type()).isEqualTo(ChessPieceType.ROOK);

        ChessMoveResult blackKingSide = rules.applyMove(
                fen("r3k2r/8/8/8/8/8/8/4K3 b kq - 0 1"), p(0, 4), p(0, 6), null);
        assertThat(blackKingSide.board().pieceAt(p(0, 5)).type()).isEqualTo(ChessPieceType.ROOK);
    }

    @Test
    void rejectsCastlingInCheckOrAcrossAttackedSquare() {
        invalid(() -> rules.applyMove(
                fen("4r2k/8/8/8/8/8/8/R3K2R w KQ - 0 1"), p(7, 4), p(7, 6), null));
        invalid(() -> rules.applyMove(
                fen("5r1k/8/8/8/8/8/8/R3K2R w KQ - 0 1"), p(7, 4), p(7, 6), null));
    }

    @Test
    void movingKingOrRookAndCapturingOriginalRookRemoveRights() {
        ChessFen position = fen("4k3/8/8/8/8/8/8/R3K2R w KQ - 0 1");
        ChessMoveResult rookMove = rules.applyMove(position, p(7, 7), p(6, 7), null);
        assertThat(rookMove.castlingRights().whiteKingSide()).isFalse();
        assertThat(rookMove.castlingRights().whiteQueenSide()).isTrue();
        ChessMoveResult kingMove = rules.applyMove(position, p(7, 4), p(6, 4), null);
        assertThat(kingMove.castlingRights().notation()).isEqualTo("-");

        ChessMoveResult capture = rules.applyMove(
                fen("r3k3/8/8/8/8/8/8/R3K3 b Q - 0 1"), p(0, 0), p(7, 0), null);
        assertThat(capture.castlingRights().whiteQueenSide()).isFalse();
    }

    @Test
    void enPassantWorksOnlyImmediatelyAndCannotExposeKing() {
        ChessMoveResult valid = rules.applyMove(
                fen("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1"), p(3, 4), p(2, 3), null);
        assertThat(valid.capture()).isTrue();
        assertThat(valid.board().pieceAt(p(3, 3))).isNull();
        invalid(() -> rules.applyMove(
                fen("4k3/8/8/3pP3/8/8/8/4K3 w - - 0 1"), p(3, 4), p(2, 3), null));
        invalid(() -> rules.applyMove(
                fen("4r2k/8/8/3pP3/8/8/8/4K3 w - d6 0 1"), p(3, 4), p(2, 3), null));
    }

    @Test
    void detectsInsufficientAndSufficientMaterial() {
        assertThat(rules.isInsufficientMaterial(board("4k3/8/8/8/8/8/8/4K3"))).isTrue();
        assertThat(rules.isInsufficientMaterial(board("4k3/8/8/8/8/8/8/2B1K3"))).isTrue();
        assertThat(rules.isInsufficientMaterial(board("4k3/8/8/8/8/8/8/2N1K3"))).isTrue();
        assertThat(rules.isInsufficientMaterial(board("4kb2/8/8/8/8/8/8/2B1K3"))).isTrue();
        assertThat(rules.isInsufficientMaterial(board("4k3/8/8/8/8/8/8/R3K3"))).isFalse();
    }

    private ChessFen fen(String value) { return ChessFen.parse(value); }
    private ChessBoard board(String value) { return ChessBoard.fromFenPlacement(value); }
    private ChessPosition p(int row, int column) { return new ChessPosition(row, column); }
    private void invalid(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOf(InvalidChessMoveException.class);
    }
}

