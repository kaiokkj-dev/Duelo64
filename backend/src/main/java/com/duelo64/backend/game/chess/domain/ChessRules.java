package com.duelo64.backend.game.chess.domain;

import java.util.List;

public class ChessRules {
    private static final List<ChessPieceType> PROMOTIONS = List.of(
            ChessPieceType.QUEEN, ChessPieceType.ROOK, ChessPieceType.BISHOP, ChessPieceType.KNIGHT);

    public ChessMoveResult applyMove(
            ChessBoard board, ChessColor currentTurn, ChessPosition from, ChessPosition to) {
        return applyMove(new ChessFen(board, currentTurn, ChessCastlingRights.none(), null, 0, 1),
                from, to, null);
    }

    public ChessMoveResult applyMove(
            ChessFen fen, ChessPosition from, ChessPosition to, ChessPieceType promotion) {
        ChessBoard board = fen.board();
        requirePosition(from);
        requirePosition(to);
        if (from.equals(to)) throw invalid("A origem e o destino precisam ser diferentes.");
        ChessPiece piece = board.pieceAt(from);
        ChessPiece target = board.pieceAt(to);
        if (piece == null) throw invalid("Nao existe peca na origem.");
        if (piece.color() != fen.activeColor()) throw invalid("Nao e a vez dessa peca.");
        if (target != null && target.color() == piece.color()) throw invalid("O destino possui uma peca da mesma cor.");
        if (target != null && target.type() == ChessPieceType.KING) throw invalid("O rei nao pode ser capturado.");

        int rowDelta = to.row() - from.row();
        int columnDelta = to.column() - from.column();
        boolean castling = piece.type() == ChessPieceType.KING && rowDelta == 0 && Math.abs(columnDelta) == 2;
        ChessPosition enPassantCaptured = null;

        if (castling) {
            validateCastling(fen, piece.color(), columnDelta > 0);
        } else if (piece.type() == ChessPieceType.PAWN && target == null
                && Math.abs(columnDelta) == 1 && rowDelta == pawnDirection(piece.color())) {
            enPassantCaptured = validateEnPassant(fen, piece, to);
        } else {
            validateGeometry(board, piece, from, to, target);
        }

        ChessPieceType appliedPromotion = validatePromotion(piece, to, promotion);
        ChessBoard next = castling
                ? board.castle(piece.color(), columnDelta > 0)
                : board.move(from, to, enPassantCaptured, appliedPromotion);
        if (isInCheck(next, piece.color())) throw invalid("A jogada deixa o proprio rei em xeque.");

        boolean capture = target != null || enPassantCaptured != null;
        ChessCastlingRights rights = updatedRights(fen.castlingRights(), piece, from, target, to);
        ChessPosition nextEnPassant = piece.type() == ChessPieceType.PAWN && Math.abs(rowDelta) == 2
                ? new ChessPosition(from.row() + pawnDirection(piece.color()), from.column())
                : null;
        return new ChessMoveResult(next, capture, piece.type() == ChessPieceType.PAWN, rights, nextEnPassant);
    }

    public boolean hasAnyLegalMove(ChessFen fen) {
        for (int fromRow = 0; fromRow < 8; fromRow++) {
            for (int fromColumn = 0; fromColumn < 8; fromColumn++) {
                ChessPosition from = new ChessPosition(fromRow, fromColumn);
                ChessPiece piece = fen.board().pieceAt(from);
                if (piece == null || piece.color() != fen.activeColor()) continue;
                for (int toRow = 0; toRow < 8; toRow++) {
                    for (int toColumn = 0; toColumn < 8; toColumn++) {
                        ChessPosition to = new ChessPosition(toRow, toColumn);
                        if (piece.type() == ChessPieceType.PAWN && (toRow == 0 || toRow == 7)) {
                            for (ChessPieceType promotion : PROMOTIONS) {
                                if (isLegal(fen, from, to, promotion)) return true;
                            }
                        } else if (isLegal(fen, from, to, null)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public List<ChessLegalMove> legalMoves(ChessFen fen, ChessPosition from) {
        requirePosition(from);
        ChessPiece piece = fen.board().pieceAt(from);
        if (piece == null || piece.color() != fen.activeColor()) return List.of();
        java.util.ArrayList<ChessLegalMove> moves = new java.util.ArrayList<>();
        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 8; column++) {
                ChessPosition to = new ChessPosition(row, column);
                boolean promotionRequired = piece.type() == ChessPieceType.PAWN && (row == 0 || row == 7);
                ChessPieceType validationPromotion = promotionRequired ? ChessPieceType.QUEEN : null;
                if (isLegal(fen, from, to, validationPromotion)) {
                    ChessPiece target = fen.board().pieceAt(to);
                    boolean enPassant = piece.type() == ChessPieceType.PAWN
                            && target == null && from.column() != to.column();
                    moves.add(new ChessLegalMove(to.row(), to.column(), target != null || enPassant, promotionRequired));
                }
            }
        }
        return List.copyOf(moves);
    }

    public boolean isInCheck(ChessBoard board, ChessColor color) {
        ChessPosition king = board.kingPosition(color);
        if (king == null) throw new IllegalStateException("O tabuleiro nao possui o rei " + color + ".");
        return isSquareAttacked(board, king, color.opponent());
    }

    public boolean isSquareAttacked(ChessBoard board, ChessPosition square, ChessColor attacker) {
        requirePosition(square);
        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 8; column++) {
                ChessPosition from = new ChessPosition(row, column);
                ChessPiece piece = board.pieceAt(from);
                if (piece != null && piece.color() == attacker && attacks(board, piece, from, square)) return true;
            }
        }
        return false;
    }

    public boolean isInsufficientMaterial(ChessBoard board) {
        java.util.ArrayList<PieceOnSquare> nonKings = new java.util.ArrayList<>();
        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 8; column++) {
                ChessPiece piece = board.pieceAt(new ChessPosition(row, column));
                if (piece != null && piece.type() != ChessPieceType.KING) {
                    nonKings.add(new PieceOnSquare(piece, new ChessPosition(row, column)));
                }
            }
        }
        if (nonKings.isEmpty()) return true;
        if (nonKings.size() == 1) {
            ChessPieceType type = nonKings.get(0).piece().type();
            return type == ChessPieceType.BISHOP || type == ChessPieceType.KNIGHT;
        }
        if (nonKings.size() == 2
                && nonKings.stream().allMatch(item -> item.piece().type() == ChessPieceType.BISHOP)
                && nonKings.get(0).piece().color() != nonKings.get(1).piece().color()) {
            ChessPosition first = nonKings.get(0).position();
            ChessPosition second = nonKings.get(1).position();
            return (first.row() + first.column()) % 2 == (second.row() + second.column()) % 2;
        }
        return false;
    }

    public boolean hasLegalEnPassant(ChessFen fen) {
        ChessPosition target = fen.enPassantTarget();
        if (target == null) return false;
        int fromRow = target.row() - pawnDirection(fen.activeColor());
        for (int column : new int[] {target.column() - 1, target.column() + 1}) {
            ChessPosition from = new ChessPosition(fromRow, column);
            if (from.isInsideBoard() && isLegal(fen, from, target, null)) return true;
        }
        return false;
    }

    private boolean isLegal(ChessFen fen, ChessPosition from, ChessPosition to, ChessPieceType promotion) {
        try {
            applyMove(fen, from, to, promotion);
            return true;
        } catch (InvalidChessMoveException | IllegalStateException exception) {
            return false;
        }
    }

    private ChessPieceType validatePromotion(ChessPiece piece, ChessPosition to, ChessPieceType promotion) {
        boolean required = piece.type() == ChessPieceType.PAWN && (to.row() == 0 || to.row() == 7);
        if (!required && promotion != null) throw invalid("Promocao informada para uma jogada que nao promove.");
        if (!required) return null;
        if (promotion == null || !PROMOTIONS.contains(promotion)) {
            throw invalid("Escolha QUEEN, ROOK, BISHOP ou KNIGHT para promover.");
        }
        return promotion;
    }

    private void validateCastling(ChessFen fen, ChessColor color, boolean kingSide) {
        ChessBoard board = fen.board();
        int row = color == ChessColor.WHITE ? 7 : 0;
        ChessPosition kingFrom = new ChessPosition(row, 4);
        ChessPosition rookFrom = new ChessPosition(row, kingSide ? 7 : 0);
        ChessPiece king = board.pieceAt(kingFrom);
        ChessPiece rook = board.pieceAt(rookFrom);
        if (!fen.castlingRights().allows(color, kingSide)
                || king == null || king.type() != ChessPieceType.KING || king.color() != color
                || rook == null || rook.type() != ChessPieceType.ROOK || rook.color() != color) {
            throw invalid("O roque nao esta mais disponivel.");
        }
        int[] emptyColumns = kingSide ? new int[] {5, 6} : new int[] {1, 2, 3};
        for (int column : emptyColumns) {
            if (board.pieceAt(new ChessPosition(row, column)) != null) throw invalid("O caminho do roque esta bloqueado.");
        }
        if (isInCheck(board, color)) throw invalid("Nao e permitido rocar em xeque.");
        int transitColumn = kingSide ? 5 : 3;
        int destinationColumn = kingSide ? 6 : 2;
        if (isSquareAttacked(board, new ChessPosition(row, transitColumn), color.opponent())
                || isSquareAttacked(board, new ChessPosition(row, destinationColumn), color.opponent())) {
            throw invalid("O rei nao pode atravessar uma casa atacada.");
        }
    }

    private ChessPosition validateEnPassant(ChessFen fen, ChessPiece pawn, ChessPosition to) {
        if (!to.equals(fen.enPassantTarget())) throw invalid("Captura diagonal invalida para o peao.");
        ChessPosition captured = new ChessPosition(to.row() - pawnDirection(pawn.color()), to.column());
        ChessPiece victim = fen.board().pieceAt(captured);
        if (victim == null || victim.type() != ChessPieceType.PAWN || victim.color() == pawn.color()) {
            throw invalid("Nao existe peao disponivel para en passant.");
        }
        return captured;
    }

    private ChessCastlingRights updatedRights(
            ChessCastlingRights rights, ChessPiece moving, ChessPosition from, ChessPiece captured, ChessPosition to) {
        ChessCastlingRights next = rights;
        if (moving.type() == ChessPieceType.KING) next = next.withoutColor(moving.color());
        if (moving.type() == ChessPieceType.ROOK) next = removeRookRightAt(next, moving.color(), from);
        if (captured != null && captured.type() == ChessPieceType.ROOK) next = removeRookRightAt(next, captured.color(), to);
        return next;
    }

    private ChessCastlingRights removeRookRightAt(
            ChessCastlingRights rights, ChessColor color, ChessPosition position) {
        int row = color == ChessColor.WHITE ? 7 : 0;
        if (position.equals(new ChessPosition(row, 0))) return rights.withoutRook(color, false);
        if (position.equals(new ChessPosition(row, 7))) return rights.withoutRook(color, true);
        return rights;
    }

    private void validateGeometry(ChessBoard board, ChessPiece piece, ChessPosition from, ChessPosition to, ChessPiece target) {
        int rowDelta = to.row() - from.row();
        int columnDelta = to.column() - from.column();
        int absRow = Math.abs(rowDelta);
        int absColumn = Math.abs(columnDelta);
        switch (piece.type()) {
            case PAWN -> validatePawn(board, piece.color(), from, target, rowDelta, absColumn);
            case KNIGHT -> { if (!((absRow == 2 && absColumn == 1) || (absRow == 1 && absColumn == 2))) throw invalid("Movimento invalido para o cavalo."); }
            case BISHOP -> { if (absRow != absColumn || !pathIsClear(board, from, to)) throw invalid("Movimento invalido para o bispo."); }
            case ROOK -> { if (!((rowDelta == 0 || columnDelta == 0) && pathIsClear(board, from, to))) throw invalid("Movimento invalido para a torre."); }
            case QUEEN -> { if (!((rowDelta == 0 || columnDelta == 0 || absRow == absColumn) && pathIsClear(board, from, to))) throw invalid("Movimento invalido para a rainha."); }
            case KING -> { if (Math.max(absRow, absColumn) != 1) throw invalid("O rei anda somente uma casa."); }
        }
    }

    private void validatePawn(ChessBoard board, ChessColor color, ChessPosition from, ChessPiece target, int rowDelta, int absColumn) {
        int direction = pawnDirection(color);
        int initialRow = color == ChessColor.WHITE ? 6 : 1;
        if (absColumn == 1 && rowDelta == direction) {
            if (target == null) throw invalid("O peao captura somente uma peca na diagonal.");
            return;
        }
        if (absColumn != 0 || target != null) throw invalid("Movimento invalido para o peao.");
        if (rowDelta == direction) return;
        if (from.row() == initialRow && rowDelta == 2 * direction
                && board.pieceAt(new ChessPosition(from.row() + direction, from.column())) == null) return;
        throw invalid("Movimento invalido para o peao.");
    }

    private boolean attacks(ChessBoard board, ChessPiece piece, ChessPosition from, ChessPosition target) {
        int rowDelta = target.row() - from.row();
        int columnDelta = target.column() - from.column();
        int absRow = Math.abs(rowDelta);
        int absColumn = Math.abs(columnDelta);
        return switch (piece.type()) {
            case PAWN -> rowDelta == pawnDirection(piece.color()) && absColumn == 1;
            case KNIGHT -> (absRow == 2 && absColumn == 1) || (absRow == 1 && absColumn == 2);
            case BISHOP -> absRow == absColumn && pathIsClear(board, from, target);
            case ROOK -> (rowDelta == 0 || columnDelta == 0) && pathIsClear(board, from, target);
            case QUEEN -> (rowDelta == 0 || columnDelta == 0 || absRow == absColumn) && pathIsClear(board, from, target);
            case KING -> Math.max(absRow, absColumn) == 1;
        };
    }

    private boolean pathIsClear(ChessBoard board, ChessPosition from, ChessPosition to) {
        int rowStep = Integer.signum(to.row() - from.row());
        int columnStep = Integer.signum(to.column() - from.column());
        int row = from.row() + rowStep;
        int column = from.column() + columnStep;
        while (row != to.row() || column != to.column()) {
            if (board.pieceAt(new ChessPosition(row, column)) != null) return false;
            row += rowStep;
            column += columnStep;
        }
        return true;
    }

    private int pawnDirection(ChessColor color) { return color == ChessColor.WHITE ? -1 : 1; }
    private void requirePosition(ChessPosition position) { if (position == null || !position.isInsideBoard()) throw invalid("Posicao fora do tabuleiro."); }
    private InvalidChessMoveException invalid(String message) { return new InvalidChessMoveException(message); }
    private record PieceOnSquare(ChessPiece piece, ChessPosition position) {}
}
