package com.duelo64.backend.game.chess.domain;

public final class ChessBoard {
    public static final int SIZE = 8;
    public static final String INITIAL_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR";

    private final ChessPiece[][] pieces;

    private ChessBoard(ChessPiece[][] pieces) {
        this.pieces = pieces;
    }

    public static ChessBoard initial() {
        return fromFenPlacement(INITIAL_FEN);
    }

    public static ChessBoard fromFenPlacement(String fen) {
        if (fen == null) throw new IllegalArgumentException("A FEN do tabuleiro e obrigatoria.");
        String[] ranks = fen.trim().split("/");
        if (ranks.length != SIZE) throw new IllegalArgumentException("A FEN precisa possuir 8 fileiras.");

        ChessPiece[][] pieces = new ChessPiece[SIZE][SIZE];
        for (int row = 0; row < SIZE; row++) {
            int column = 0;
            for (char symbol : ranks[row].toCharArray()) {
                if (Character.isDigit(symbol)) {
                    int empty = symbol - '0';
                    if (empty < 1 || empty > 8) throw invalidFen();
                    column += empty;
                } else {
                    if (column >= SIZE) throw invalidFen();
                    pieces[row][column++] = pieceFromSymbol(symbol);
                }
            }
            if (column != SIZE) throw invalidFen();
        }
        return new ChessBoard(pieces);
    }

    public String toFenPlacement() {
        StringBuilder fen = new StringBuilder();
        for (int row = 0; row < SIZE; row++) {
            if (row > 0) fen.append('/');
            int empty = 0;
            for (int column = 0; column < SIZE; column++) {
                ChessPiece piece = pieces[row][column];
                if (piece == null) {
                    empty++;
                } else {
                    if (empty > 0) fen.append(empty);
                    empty = 0;
                    fen.append(symbolFor(piece));
                }
            }
            if (empty > 0) fen.append(empty);
        }
        return fen.toString();
    }

    public ChessPiece pieceAt(ChessPosition position) {
        requireInside(position);
        return pieces[position.row()][position.column()];
    }

    public ChessBoard move(ChessPosition from, ChessPosition to) {
        return move(from, to, null, null);
    }

    public ChessBoard move(
            ChessPosition from,
            ChessPosition to,
            ChessPosition additionallyCaptured,
            ChessPieceType promotion) {
        requireInside(from);
        requireInside(to);
        ChessPiece[][] copy = copyPieces();
        ChessPiece moving = copy[from.row()][from.column()];
        copy[to.row()][to.column()] = promotion == null
                ? moving
                : new ChessPiece(moving.color(), promotion);
        copy[from.row()][from.column()] = null;
        if (additionallyCaptured != null) {
            copy[additionallyCaptured.row()][additionallyCaptured.column()] = null;
        }
        return new ChessBoard(copy);
    }

    public ChessBoard castle(ChessColor color, boolean kingSide) {
        int row = color == ChessColor.WHITE ? 7 : 0;
        ChessPosition kingFrom = new ChessPosition(row, 4);
        ChessPosition kingTo = new ChessPosition(row, kingSide ? 6 : 2);
        ChessPosition rookFrom = new ChessPosition(row, kingSide ? 7 : 0);
        ChessPosition rookTo = new ChessPosition(row, kingSide ? 5 : 3);
        ChessPiece[][] copy = copyPieces();
        copy[kingTo.row()][kingTo.column()] = copy[kingFrom.row()][kingFrom.column()];
        copy[kingFrom.row()][kingFrom.column()] = null;
        copy[rookTo.row()][rookTo.column()] = copy[rookFrom.row()][rookFrom.column()];
        copy[rookFrom.row()][rookFrom.column()] = null;
        return new ChessBoard(copy);
    }

    public ChessPosition kingPosition(ChessColor color) {
        for (int row = 0; row < SIZE; row++) {
            for (int column = 0; column < SIZE; column++) {
                ChessPiece piece = pieces[row][column];
                if (piece != null && piece.color() == color && piece.type() == ChessPieceType.KING) {
                    return new ChessPosition(row, column);
                }
            }
        }
        return null;
    }

    private ChessPiece[][] copyPieces() {
        ChessPiece[][] copy = new ChessPiece[SIZE][SIZE];
        for (int row = 0; row < SIZE; row++) {
            System.arraycopy(pieces[row], 0, copy[row], 0, SIZE);
        }
        return copy;
    }

    private static ChessPiece pieceFromSymbol(char symbol) {
        ChessColor color = Character.isUpperCase(symbol) ? ChessColor.WHITE : ChessColor.BLACK;
        ChessPieceType type = switch (Character.toLowerCase(symbol)) {
            case 'k' -> ChessPieceType.KING;
            case 'q' -> ChessPieceType.QUEEN;
            case 'r' -> ChessPieceType.ROOK;
            case 'b' -> ChessPieceType.BISHOP;
            case 'n' -> ChessPieceType.KNIGHT;
            case 'p' -> ChessPieceType.PAWN;
            default -> throw invalidFen();
        };
        return new ChessPiece(color, type);
    }

    private static char symbolFor(ChessPiece piece) {
        char symbol = switch (piece.type()) {
            case KING -> 'k';
            case QUEEN -> 'q';
            case ROOK -> 'r';
            case BISHOP -> 'b';
            case KNIGHT -> 'n';
            case PAWN -> 'p';
        };
        return piece.color() == ChessColor.WHITE ? Character.toUpperCase(symbol) : symbol;
    }

    private static void requireInside(ChessPosition position) {
        if (position == null || !position.isInsideBoard()) {
            throw new IllegalArgumentException("A posicao esta fora do tabuleiro.");
        }
    }

    private static IllegalArgumentException invalidFen() {
        return new IllegalArgumentException("A FEN do tabuleiro e invalida.");
    }
}
