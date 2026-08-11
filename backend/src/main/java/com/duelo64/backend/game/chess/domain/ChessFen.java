package com.duelo64.backend.game.chess.domain;

public record ChessFen(
        ChessBoard board,
        ChessColor activeColor,
        ChessCastlingRights castlingRights,
        ChessPosition enPassantTarget,
        int halfmoveClock,
        int fullmoveNumber) {

    public static ChessFen initial() {
        return new ChessFen(ChessBoard.initial(), ChessColor.WHITE,
                ChessCastlingRights.initial(), null, 0, 1);
    }

    public static ChessFen parse(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("A FEN e obrigatoria.");
        String[] fields = value.trim().split("\\s+");
        if (fields.length == 1) {
            return new ChessFen(ChessBoard.fromFenPlacement(fields[0]), ChessColor.WHITE,
                    ChessCastlingRights.initial(), null, 0, 1);
        }
        if (fields.length != 6) throw new IllegalArgumentException("A FEN completa precisa possuir 6 campos.");
        ChessColor active = switch (fields[1]) {
            case "w" -> ChessColor.WHITE;
            case "b" -> ChessColor.BLACK;
            default -> throw new IllegalArgumentException("Cor ativa invalida na FEN.");
        };
        int halfmove = Integer.parseInt(fields[4]);
        int fullmove = Integer.parseInt(fields[5]);
        if (halfmove < 0 || fullmove < 1) throw new IllegalArgumentException("Contadores invalidos na FEN.");
        return new ChessFen(
                ChessBoard.fromFenPlacement(fields[0]), active,
                ChessCastlingRights.parse(fields[2]), parseSquare(fields[3]), halfmove, fullmove);
    }

    public String notation() {
        return board.toFenPlacement() + " "
                + (activeColor == ChessColor.WHITE ? "w" : "b") + " "
                + castlingRights.notation() + " "
                + squareNotation(enPassantTarget) + " "
                + halfmoveClock + " " + fullmoveNumber;
    }

    public String repetitionIdentity(boolean enPassantLegallyRelevant) {
        return board.toFenPlacement() + "|" + activeColor + "|" + castlingRights.notation()
                + "|" + (enPassantLegallyRelevant ? squareNotation(enPassantTarget) : "-");
    }

    private static ChessPosition parseSquare(String value) {
        if (value.equals("-")) return null;
        if (value.length() != 2 || value.charAt(0) < 'a' || value.charAt(0) > 'h'
                || value.charAt(1) < '1' || value.charAt(1) > '8') {
            throw new IllegalArgumentException("Casa en passant invalida na FEN.");
        }
        return new ChessPosition(8 - (value.charAt(1) - '0'), value.charAt(0) - 'a');
    }

    private static String squareNotation(ChessPosition position) {
        if (position == null) return "-";
        return "" + (char) ('a' + position.column()) + (8 - position.row());
    }
}
