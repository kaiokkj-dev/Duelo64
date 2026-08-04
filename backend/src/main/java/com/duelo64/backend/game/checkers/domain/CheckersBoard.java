package com.duelo64.backend.game.checkers.domain;

import java.util.ArrayList;
import java.util.List;

public final class CheckersBoard {

    public static final int SIZE = 8;
    public static final String EMPTY_CELL = ".";

    private final Piece[][] pieces;

    private CheckersBoard(Piece[][] pieces) {
        this.pieces = pieces;
    }

    public static CheckersBoard initial() {
        Piece[][] pieces = new Piece[SIZE][SIZE];

        for (int row = 0; row < 3; row++) {
            fillPlayableRow(pieces, row, PieceColor.BLACK);
        }

        for (int row = 5; row < SIZE; row++) {
            fillPlayableRow(pieces, row, PieceColor.WHITE);
        }

        return new CheckersBoard(pieces);
    }

    public static CheckersBoard fromNotation(String notation) {
        if (notation == null || notation.length() != SIZE * SIZE) {
            throw new IllegalArgumentException("A notacao do tabuleiro precisa ter 64 caracteres.");
        }

        Piece[][] pieces = new Piece[SIZE][SIZE];

        for (int index = 0; index < notation.length(); index++) {
            int row = index / SIZE;
            int column = index % SIZE;
            char symbol = notation.charAt(index);

            pieces[row][column] = switch (symbol) {
                case 'w' -> new Piece(PieceColor.WHITE, false);
                case 'W' -> new Piece(PieceColor.WHITE, true);
                case 'b' -> new Piece(PieceColor.BLACK, false);
                case 'B' -> new Piece(PieceColor.BLACK, true);
                case '.' -> null;
                default -> throw new IllegalArgumentException("Peca invalida na notacao do tabuleiro.");
            };
        }

        return new CheckersBoard(pieces);
    }

    public String toNotation() {
        StringBuilder notation = new StringBuilder(SIZE * SIZE);

        for (int row = 0; row < SIZE; row++) {
            for (int column = 0; column < SIZE; column++) {
                Piece piece = pieces[row][column];
                notation.append(toSymbol(piece));
            }
        }

        return notation.toString();
    }

    public List<List<String>> toMatrix() {
        List<List<String>> matrix = new ArrayList<>();

        for (int row = 0; row < SIZE; row++) {
            List<String> rowCells = new ArrayList<>();

            for (int column = 0; column < SIZE; column++) {
                Piece piece = pieces[row][column];
                rowCells.add(piece == null ? EMPTY_CELL : String.valueOf(toSymbol(piece)));
            }

            matrix.add(rowCells);
        }

        return matrix;
    }

    public Piece pieceAt(BoardPosition position) {
        return pieces[position.row()][position.column()];
    }

    public CheckersBoard move(BoardPosition from, BoardPosition to) {
        Piece[][] nextPieces = copyPieces();
        Piece piece = nextPieces[from.row()][from.column()];

        nextPieces[from.row()][from.column()] = null;

        if (Math.abs(to.row() - from.row()) == 2) {
            int capturedRow = (from.row() + to.row()) / 2;
            int capturedColumn = (from.column() + to.column()) / 2;
            nextPieces[capturedRow][capturedColumn] = null;
        }

        nextPieces[to.row()][to.column()] = promoteIfNeeded(piece, to);

        return new CheckersBoard(nextPieces);
    }

    private static void fillPlayableRow(Piece[][] pieces, int row, PieceColor color) {
        for (int column = 0; column < SIZE; column++) {
            if (isDarkSquare(row, column)) {
                pieces[row][column] = new Piece(color, false);
            }
        }
    }

    public static boolean isDarkSquare(int row, int column) {
        return (row + column) % 2 == 1;
    }

    private Piece[][] copyPieces() {
        Piece[][] copy = new Piece[SIZE][SIZE];

        for (int row = 0; row < SIZE; row++) {
            System.arraycopy(pieces[row], 0, copy[row], 0, SIZE);
        }

        return copy;
    }

    private Piece promoteIfNeeded(Piece piece, BoardPosition to) {
        if (piece.getColor() == PieceColor.WHITE && to.row() == 0) {
            return piece.promote();
        }

        if (piece.getColor() == PieceColor.BLACK && to.row() == SIZE - 1) {
            return piece.promote();
        }

        return piece;
    }

    private char toSymbol(Piece piece) {
        if (piece == null) {
            return '.';
        }

        if (piece.getColor() == PieceColor.WHITE) {
            return piece.isKing() ? 'W' : 'w';
        }

        return piece.isKing() ? 'B' : 'b';
    }
}
