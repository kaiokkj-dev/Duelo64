package com.duelo64.backend.game.chess.domain;

public record ChessCastlingRights(
        boolean whiteKingSide,
        boolean whiteQueenSide,
        boolean blackKingSide,
        boolean blackQueenSide) {

    public static ChessCastlingRights initial() {
        return new ChessCastlingRights(true, true, true, true);
    }

    public static ChessCastlingRights none() {
        return new ChessCastlingRights(false, false, false, false);
    }

    public static ChessCastlingRights parse(String value) {
        if (value == null || value.equals("-")) return none();
        return new ChessCastlingRights(
                value.contains("K"), value.contains("Q"), value.contains("k"), value.contains("q"));
    }

    public String notation() {
        StringBuilder value = new StringBuilder();
        if (whiteKingSide) value.append('K');
        if (whiteQueenSide) value.append('Q');
        if (blackKingSide) value.append('k');
        if (blackQueenSide) value.append('q');
        return value.isEmpty() ? "-" : value.toString();
    }

    public boolean allows(ChessColor color, boolean kingSide) {
        if (color == ChessColor.WHITE) return kingSide ? whiteKingSide : whiteQueenSide;
        return kingSide ? blackKingSide : blackQueenSide;
    }

    public ChessCastlingRights withoutColor(ChessColor color) {
        return color == ChessColor.WHITE
                ? new ChessCastlingRights(false, false, blackKingSide, blackQueenSide)
                : new ChessCastlingRights(whiteKingSide, whiteQueenSide, false, false);
    }

    public ChessCastlingRights withoutRook(ChessColor color, boolean kingSide) {
        if (color == ChessColor.WHITE) {
            return kingSide
                    ? new ChessCastlingRights(false, whiteQueenSide, blackKingSide, blackQueenSide)
                    : new ChessCastlingRights(whiteKingSide, false, blackKingSide, blackQueenSide);
        }
        return kingSide
                ? new ChessCastlingRights(whiteKingSide, whiteQueenSide, false, blackQueenSide)
                : new ChessCastlingRights(whiteKingSide, whiteQueenSide, blackKingSide, false);
    }
}
