package com.duelo64.backend.game.checkers.domain;

public class CheckersRules {

    public CheckersBoard applyMove(
            CheckersBoard board,
            PieceColor currentTurn,
            BoardPosition from,
            BoardPosition to) {

        validatePositions(from, to);

        Piece piece = board.pieceAt(from);

        if (piece == null) {
            throw new InvalidCheckersMoveException("Nao existe peca nessa casa.");
        }

        if (piece.getColor() != currentTurn) {
            throw new InvalidCheckersMoveException("Nao e a vez dessa cor jogar.");
        }

        if (board.pieceAt(to) != null) {
            throw new InvalidCheckersMoveException("A casa de destino ja esta ocupada.");
        }

        if (!CheckersBoard.isDarkSquare(to.row(), to.column())) {
            throw new InvalidCheckersMoveException("A peca precisa parar em uma casa escura.");
        }

        int rowDelta = to.row() - from.row();
        int columnDelta = to.column() - from.column();

        if (Math.abs(rowDelta) != Math.abs(columnDelta)) {
            throw new InvalidCheckersMoveException("A dama se move apenas na diagonal.");
        }

        if (Math.abs(rowDelta) == 1) {
            validateSimpleMove(board, currentTurn, piece, rowDelta);
            return board.move(from, to);
        }

        if (Math.abs(rowDelta) == 2) {
            validateCaptureMove(board, piece, from, to, rowDelta, columnDelta);
            return board.move(from, to);
        }

        throw new InvalidCheckersMoveException("Movimento invalido para esta peca.");
    }

    private void validatePositions(BoardPosition from, BoardPosition to) {
        if (!from.isInsideBoard() || !to.isInsideBoard()) {
            throw new InvalidCheckersMoveException("A posicao informada esta fora do tabuleiro.");
        }
    }

    private void validateSimpleMove(
            CheckersBoard board,
            PieceColor currentTurn,
            Piece piece,
            int rowDelta) {

        if (hasAnyCapture(board, currentTurn)) {
            throw new InvalidCheckersMoveException("Existe captura obrigatoria disponivel.");
        }

        if (!piece.isKing() && rowDelta != forwardDirection(piece.getColor())) {
            throw new InvalidCheckersMoveException("Essa peca so pode andar para frente.");
        }
    }

    private void validateCaptureMove(
            CheckersBoard board,
            Piece piece,
            BoardPosition from,
            BoardPosition to,
            int rowDelta,
            int columnDelta) {

        if (!piece.isKing() && rowDelta != forwardDirection(piece.getColor()) * 2) {
            throw new InvalidCheckersMoveException("Essa peca so pode capturar para frente.");
        }

        BoardPosition capturedPosition = new BoardPosition(
                (from.row() + to.row()) / 2,
                (from.column() + to.column()) / 2);

        Piece capturedPiece = board.pieceAt(capturedPosition);

        if (capturedPiece == null) {
            throw new InvalidCheckersMoveException("Nao existe peca para capturar nesse movimento.");
        }

        if (capturedPiece.getColor() == piece.getColor()) {
            throw new InvalidCheckersMoveException("Voce nao pode capturar a propria peca.");
        }
    }

    private boolean hasAnyCapture(CheckersBoard board, PieceColor color) {
        for (int row = 0; row < CheckersBoard.SIZE; row++) {
            for (int column = 0; column < CheckersBoard.SIZE; column++) {
                BoardPosition from = new BoardPosition(row, column);
                Piece piece = board.pieceAt(from);

                if (piece != null && piece.getColor() == color && canCaptureFrom(board, from, piece)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean canCaptureFrom(CheckersBoard board, BoardPosition from, Piece piece) {
        int[][] directions = piece.isKing()
                ? new int[][] { { -2, -2 }, { -2, 2 }, { 2, -2 }, { 2, 2 } }
                : new int[][] { { forwardDirection(piece.getColor()) * 2, -2 },
                        { forwardDirection(piece.getColor()) * 2, 2 } };

        for (int[] direction : directions) {
            BoardPosition to = new BoardPosition(from.row() + direction[0], from.column() + direction[1]);

            if (!to.isInsideBoard() || board.pieceAt(to) != null) {
                continue;
            }

            BoardPosition middle = new BoardPosition(
                    (from.row() + to.row()) / 2,
                    (from.column() + to.column()) / 2);
            Piece capturedPiece = board.pieceAt(middle);

            if (capturedPiece != null && capturedPiece.getColor() != piece.getColor()) {
                return true;
            }
        }

        return false;
    }

    private int forwardDirection(PieceColor color) {
        return color == PieceColor.WHITE ? -1 : 1;
    }
}
