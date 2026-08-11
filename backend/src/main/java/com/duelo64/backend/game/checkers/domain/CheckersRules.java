package com.duelo64.backend.game.checkers.domain;

import java.util.ArrayList;
import java.util.List;

public class CheckersRules {

    private static final int[][] DIAGONALS = {
            { -1, -1 }, { -1, 1 }, { 1, -1 }, { 1, 1 }
    };

    public CheckersBoard applyMove(
            CheckersBoard board,
            PieceColor currentTurn,
            BoardPosition from,
            BoardPosition to) {

        return applyMoveDetailed(board, currentTurn, from, to, null).board();
    }

    public CheckersMoveResult applyMoveDetailed(
            CheckersBoard board,
            PieceColor currentTurn,
            BoardPosition from,
            BoardPosition to,
            BoardPosition forcedPiece) {

        validatePositions(from, to);

        if (forcedPiece != null && !forcedPiece.equals(from)) {
            throw new InvalidCheckersMoveException("A mesma peca deve continuar a captura.");
        }

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
        if (rowDelta == 0 || Math.abs(rowDelta) != Math.abs(columnDelta)) {
            throw new InvalidCheckersMoveException("A peca se move apenas na diagonal.");
        }

        List<CaptureOption> requestedOptions = captureOptions(board, from, piece);
        CaptureOption requestedCapture = requestedOptions.stream()
                .filter(option -> option.to().equals(to))
                .findFirst()
                .orElse(null);

        int requiredCaptureCount = forcedPiece == null
                ? maximumCaptureCount(board, currentTurn)
                : maximumCapturesFrom(board, from, piece);

        if (requiredCaptureCount > 0) {
            if (requestedCapture == null) {
                throw new InvalidCheckersMoveException("Existe captura obrigatoria disponivel.");
            }

            CheckersBoard afterCapture = board.move(from, to, requestedCapture.captured(), false);
            int continuationCount = maximumCapturesFrom(afterCapture, to, piece);
            int chosenCaptureCount = 1 + continuationCount;

            if (chosenCaptureCount < requiredCaptureCount) {
                throw new InvalidCheckersMoveException(
                        "A Lei da Maioria exige a sequencia que captura mais pecas.");
            }

            boolean mustContinue = continuationCount > 0;
            CheckersBoard resultingBoard = mustContinue
                    ? afterCapture
                    : afterCapture.promoteAt(to);

            return new CheckersMoveResult(resultingBoard, true, mustContinue, to);
        }

        validateSimpleMove(board, piece, from, to);
        return new CheckersMoveResult(board.move(from, to, null, true), false, false, to);
    }

    public List<CheckersLegalMove> legalMoves(
            CheckersBoard board,
            PieceColor currentTurn,
            BoardPosition from,
            BoardPosition forcedPiece) {

        List<CheckersLegalMove> moves = new ArrayList<>();

        for (int row = 0; row < CheckersBoard.SIZE; row++) {
            for (int column = 0; column < CheckersBoard.SIZE; column++) {
                BoardPosition to = new BoardPosition(row, column);

                try {
                    CheckersMoveResult result = applyMoveDetailed(
                            board,
                            currentTurn,
                            from,
                            to,
                            forcedPiece);
                    moves.add(new CheckersLegalMove(to, result.capture()));
                } catch (InvalidCheckersMoveException ignored) {
                    // A mesma autoridade usada no POST elimina destinos invalidos.
                }
            }
        }

        return List.copyOf(moves);
    }

    public boolean hasAnyLegalMove(CheckersBoard board, PieceColor color) {
        if (maximumCaptureCount(board, color) > 0) {
            return true;
        }

        for (int row = 0; row < CheckersBoard.SIZE; row++) {
            for (int column = 0; column < CheckersBoard.SIZE; column++) {
                BoardPosition from = new BoardPosition(row, column);
                Piece piece = board.pieceAt(from);
                if (piece != null && piece.getColor() == color && hasSimpleMove(board, from, piece)) {
                    return true;
                }
            }
        }

        return false;
    }

    public int maximumCaptureCount(CheckersBoard board, PieceColor color) {
        int maximum = 0;

        for (int row = 0; row < CheckersBoard.SIZE; row++) {
            for (int column = 0; column < CheckersBoard.SIZE; column++) {
                BoardPosition from = new BoardPosition(row, column);
                Piece piece = board.pieceAt(from);
                if (piece != null && piece.getColor() == color) {
                    maximum = Math.max(maximum, maximumCapturesFrom(board, from, piece));
                }
            }
        }

        return maximum;
    }

    private int maximumCapturesFrom(CheckersBoard board, BoardPosition from, Piece piece) {
        int maximum = 0;

        for (CaptureOption option : captureOptions(board, from, piece)) {
            CheckersBoard next = board.move(from, option.to(), option.captured(), false);
            maximum = Math.max(maximum, 1 + maximumCapturesFrom(next, option.to(), piece));
        }

        return maximum;
    }

    private List<CaptureOption> captureOptions(CheckersBoard board, BoardPosition from, Piece piece) {
        return piece.isKing()
                ? kingCaptureOptions(board, from, piece)
                : manCaptureOptions(board, from, piece);
    }

    private List<CaptureOption> manCaptureOptions(CheckersBoard board, BoardPosition from, Piece piece) {
        List<CaptureOption> options = new ArrayList<>();

        for (int[] direction : DIAGONALS) {
            BoardPosition captured = new BoardPosition(
                    from.row() + direction[0],
                    from.column() + direction[1]);
            BoardPosition to = new BoardPosition(
                    from.row() + direction[0] * 2,
                    from.column() + direction[1] * 2);

            if (!captured.isInsideBoard() || !to.isInsideBoard() || board.pieceAt(to) != null) {
                continue;
            }

            Piece capturedPiece = board.pieceAt(captured);
            if (capturedPiece != null && capturedPiece.getColor() != piece.getColor()) {
                options.add(new CaptureOption(to, captured));
            }
        }

        return options;
    }

    private List<CaptureOption> kingCaptureOptions(CheckersBoard board, BoardPosition from, Piece piece) {
        List<CaptureOption> options = new ArrayList<>();

        for (int[] direction : DIAGONALS) {
            BoardPosition captured = null;
            int row = from.row() + direction[0];
            int column = from.column() + direction[1];

            while (new BoardPosition(row, column).isInsideBoard()) {
                BoardPosition position = new BoardPosition(row, column);
                Piece occupant = board.pieceAt(position);

                if (occupant == null) {
                    if (captured != null) {
                        options.add(new CaptureOption(position, captured));
                    }
                } else if (occupant.getColor() == piece.getColor() || captured != null) {
                    break;
                } else {
                    captured = position;
                }

                row += direction[0];
                column += direction[1];
            }
        }

        return options;
    }

    private void validateSimpleMove(
            CheckersBoard board,
            Piece piece,
            BoardPosition from,
            BoardPosition to) {

        int rowDelta = to.row() - from.row();
        int columnDelta = to.column() - from.column();

        if (!piece.isKing()) {
            if (Math.abs(columnDelta) != 1 || rowDelta != forwardDirection(piece.getColor())) {
                throw new InvalidCheckersMoveException("Essa pedra so pode andar uma casa para frente.");
            }
            return;
        }

        int rowStep = Integer.signum(rowDelta);
        int columnStep = Integer.signum(columnDelta);
        int row = from.row() + rowStep;
        int column = from.column() + columnStep;

        while (row != to.row()) {
            if (board.pieceAt(new BoardPosition(row, column)) != null) {
                throw new InvalidCheckersMoveException("A dama nao pode atravessar outra peca.");
            }
            row += rowStep;
            column += columnStep;
        }
    }

    private boolean hasSimpleMove(CheckersBoard board, BoardPosition from, Piece piece) {
        int[][] directions = piece.isKing()
                ? DIAGONALS
                : new int[][] {
                        { forwardDirection(piece.getColor()), -1 },
                        { forwardDirection(piece.getColor()), 1 }
                };

        for (int[] direction : directions) {
            BoardPosition to = new BoardPosition(
                    from.row() + direction[0],
                    from.column() + direction[1]);
            if (to.isInsideBoard() && board.pieceAt(to) == null) {
                return true;
            }
        }

        return false;
    }

    private void validatePositions(BoardPosition from, BoardPosition to) {
        if (!from.isInsideBoard() || !to.isInsideBoard()) {
            throw new InvalidCheckersMoveException("A posicao informada esta fora do tabuleiro.");
        }
    }

    private int forwardDirection(PieceColor color) {
        return color == PieceColor.WHITE ? -1 : 1;
    }

    private record CaptureOption(BoardPosition to, BoardPosition captured) {
    }
}
