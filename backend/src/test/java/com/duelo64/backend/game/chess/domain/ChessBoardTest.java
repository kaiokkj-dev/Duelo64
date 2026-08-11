package com.duelo64.backend.game.chess.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ChessBoardTest {
    @Test
    void initialPositionUsesOfficialChessSetupAndStableFen() {
        ChessBoard board = ChessBoard.initial();

        assertThat(board.toFenPlacement()).isEqualTo(ChessBoard.INITIAL_FEN);
        assertThat(board.pieceAt(new ChessPosition(7, 4)))
                .isEqualTo(new ChessPiece(ChessColor.WHITE, ChessPieceType.KING));
        assertThat(board.pieceAt(new ChessPosition(0, 3)))
                .isEqualTo(new ChessPiece(ChessColor.BLACK, ChessPieceType.QUEEN));
        assertThat(board.pieceAt(new ChessPosition(6, 0)).type()).isEqualTo(ChessPieceType.PAWN);
        assertThat(board.pieceAt(new ChessPosition(4, 4))).isNull();
    }

    @Test
    void fenRoundTripPreservesPieces() {
        String fen = "4k3/8/2n5/8/3Q4/8/8/4K3";
        assertThat(ChessBoard.fromFenPlacement(fen).toFenPlacement()).isEqualTo(fen);
    }
}

