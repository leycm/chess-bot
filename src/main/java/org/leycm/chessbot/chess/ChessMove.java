package org.leycm.chessbot.chess;

import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.leycm.chessbot.chess.pieces.PawnChessPiece;

import java.util.Arrays;
import java.util.List;

@Getter
public class ChessMove {
    private final ChessPiece movedPiece;
    private final ChessPiece capturedPiece;
    private final int fromX, fromY;
    private final int toX, toY;
    private final boolean isPromotion;
    private final char promotionPiece;
    private final boolean isCastling;
    private final boolean isEnPassant;

    private final ChessBoard board;

    @Contract("_, _ -> new")
    public static @NotNull ChessMove algebraic(@NotNull String algebraic, ChessBoard board) {
        // check (+), checkmate (#) symbols
        if (algebraic.endsWith("+") || algebraic.endsWith("#")) {
            algebraic = algebraic.substring(0, algebraic.length() - 1);
        }

        // castling
        if (algebraic.equals("O-O") || algebraic.equals("0-0")) {
            return castling(true, board.isWhiteTurn(), board);
        }
        if (algebraic.equals("O-O-O") || algebraic.equals("0-0-0")) {
            return castling(false, board.isWhiteTurn(), board);
        }

        // pawn moves (2 characters: e4, e5)
        if (algebraic.length() == 2 && Character.isLowerCase(algebraic.charAt(0))) {
            return pawnAlgebraic(algebraic.toUpperCase(), board.isWhiteTurn(), board);
        }

        // pawn captures (4 characters: exd5)
        if (algebraic.length() == 4 && Character.isLowerCase(algebraic.charAt(0)) && algebraic.charAt(1) == 'x') {
            return pawnCapture(algebraic.toUpperCase(), board.isWhiteTurn(), board);
        }

        // (3-6 characters: e8=Q, exd8=Q+)
        if (algebraic.contains("=")) {
            return pawnPromotion(algebraic.toUpperCase(), board.isWhiteTurn(), board);
        }

        if (algebraic.length() == 3) {
            // simple piece move (Nf3, Be2)
            return pieceAlgebraic(algebraic.toUpperCase(), board.isWhiteTurn(), board);
        }

        if (algebraic.length() == 4) {
            if (algebraic.charAt(1) == 'x') {
                // piece capture (Nxf3, Bxe2)
                return pieceCapture(algebraic.toUpperCase(), board.isWhiteTurn(), board);
            } else {
                // piece move (Nbd7, N1f3, Rae1)
                return disambiguatedPiece(algebraic.toUpperCase(), board.isWhiteTurn(), board);
            }
        }

        if (algebraic.length() == 5) {
            if (algebraic.charAt(2) == 'x') {
                // capture (Nbxd7, N1xf3)
                return disambiguatedCapture(algebraic.toUpperCase(), board.isWhiteTurn(), board);
            } else {
                // 5-character
                return disambiguatedPiece(algebraic.toUpperCase(), board.isWhiteTurn(), board);
            }
        }

        if (algebraic.length() == 6) {
            // capture (Qh4xf2)
            return disambiguatedCapture(algebraic.toUpperCase(), board.isWhiteTurn(), board);
        }

        return new ChessMove(-1, -1, -1, -1, board, algebraic);
    }

    public ChessMove(int fromX, int fromY, int toX, int toY, @NotNull ChessBoard board, String algebraic) {
        this(fromX, fromY, toX, toY, board, false, '\0', false, false, algebraic);
    }

    public ChessMove(int fromX, int fromY, int toX, int toY, @NotNull ChessBoard board,
                     boolean isPromotion, char promotionPiece, boolean isCastling, boolean isEnPassant, String algebraic) {
        this.movedPiece = fromX != -1 ? board.getPiece(fromX, fromY) : null;
        this.capturedPiece = toX != -1 ? board.getPiece(toX, toY) : null;

        this.fromX = fromX;
        this.fromY = fromY;
        this.toX = toX;
        this.toY = toY;

        this.isPromotion = isPromotion;
        this.promotionPiece = promotionPiece;
        this.isCastling = isCastling;
        this.isEnPassant = isEnPassant;

        this.board = board;
    }

    public String asCoordinate() {
        return (char)(fromX + 'A') +
                (8 - fromY) + "-" +
                (char)(toX + 'A') +
                (8 - toY);
    }

    public String asFigurineAlgebraic() {
        return (char)(fromX + 'A') +
                (8 - fromY) + "-" +
                (char)(toX + 'A') +
                (8 - toY);
    }

    @Contract("_, _, _ -> new")
    private static @NotNull ChessMove pawnAlgebraic(@NotNull String algebraic, boolean whiteTurn, @NotNull ChessBoard board) {
        int toX = algebraic.charAt(0) - 'A';
        int toY = 8 - (algebraic.charAt(1) - '0');

        int fromX = toX;
        int fromY = toY + (whiteTurn ? 1 : -1);

        if (board.getPiece(fromX, fromY) == null ||
                (board.getPiece(fromX, fromY) instanceof PawnChessPiece)) {
            fromY = whiteTurn ? 6 : 1;
        }

        return new ChessMove(fromX, fromY, toX, toY, board, algebraic);
    }

    @Contract("_, _, _ -> new")
    private static @NotNull ChessMove pawnCapture(@NotNull String algebraic, boolean whiteTurn, @NotNull ChessBoard board) {
        int fromX = algebraic.charAt(0) - 'A';
        int toX = algebraic.charAt(2) - 'A';
        int toY = 8 - (algebraic.charAt(3) - '0');
        int fromY = toY + (whiteTurn ? 1 : -1);

        boolean isEnPassant = board.getPiece(toX, toY) == null &&
                board.getPiece(toX, fromY) != null &&
                board.getPiece(toX, fromY) instanceof PawnChessPiece;

        return new ChessMove(fromX, fromY, toX, toY, board, false, '\0', false, isEnPassant, algebraic);
    }

    @Contract("_, _, _ -> new")
    private static @NotNull ChessMove pawnPromotion(@NotNull String algebraic, boolean whiteTurn, @NotNull ChessBoard board) {
        int equalIndex = algebraic.indexOf('=');
        char promotionPiece = algebraic.charAt(equalIndex + 1);
        String movepart = algebraic.substring(0, equalIndex);

        int toX, toY, fromX, fromY;

        if (movepart.contains("x")) {
            // exd8=Q
            fromX = movepart.charAt(0) - 'A';
            toX = movepart.charAt(2) - 'A';
            toY = 8 - (movepart.charAt(3) - '0');
        } else {
            // e8=Q
            toX = movepart.charAt(0) - 'A';
            toY = 8 - (movepart.charAt(1) - '0');
            fromX = toX;
        }
        fromY = toY + (whiteTurn ? 1 : -1);

        return new ChessMove(fromX, fromY, toX, toY, board, true, promotionPiece, false, false, algebraic);
    }

    @Contract("_, _, _ -> new")
    private static @NotNull ChessMove pieceAlgebraic(@NotNull String algebraic, boolean whiteTurn, @NotNull ChessBoard board) {
        char pieceChar = algebraic.charAt(0);
        int toX = algebraic.charAt(1) - 'A';
        int toY = 8 - (algebraic.charAt(2) - '0');

        ChessPiece movedPiece = findPieceCanMoveTo(pieceChar, toX, toY, whiteTurn, board, null);

        if (movedPiece == null) return new ChessMove(-1, -1, -1, -1, board, algebraic);

        return new ChessMove(movedPiece.getX(), movedPiece.getY(), toX, toY, board, algebraic);
    }

    @Contract("_, _, _ -> new")
    private static @NotNull ChessMove pieceCapture(@NotNull String algebraic, boolean whiteTurn, @NotNull ChessBoard board) {
        char pieceChar = algebraic.charAt(0);
        int toX = algebraic.charAt(2) - 'A';
        int toY = 8 - (algebraic.charAt(3) - '0');

        ChessPiece movedPiece = findPieceCanMoveTo(pieceChar, toX, toY, whiteTurn, board, null);

        if (movedPiece == null) return new ChessMove(-1, -1, -1, -1, board, algebraic);

        return new ChessMove(movedPiece.getX(), movedPiece.getY(), toX, toY, board, algebraic);
    }

    @Contract("_, _, _ -> new")
    private static @NotNull ChessMove disambiguatedPiece(@NotNull String algebraic, boolean whiteTurn, @NotNull ChessBoard board) {
        char pieceChar = algebraic.charAt(0);
        char disambiguator = algebraic.charAt(1);
        int toX = algebraic.charAt(2) - 'A';
        int toY = 8 - (algebraic.charAt(3) - '0');

        ChessPiece movedPiece = findPieceCanMoveTo(pieceChar, toX, toY, whiteTurn, board, disambiguator);

        if (movedPiece == null) return new ChessMove(-1, -1, -1, -1, board, algebraic);

        return new ChessMove(movedPiece.getX(), movedPiece.getY(), toX, toY, board, algebraic);
    }

    @Contract("_, _, _ -> new")
    private static @NotNull ChessMove disambiguatedCapture(@NotNull String algebraic, boolean whiteTurn, @NotNull ChessBoard board) {
        char pieceChar = algebraic.charAt(0);
        char disambiguator = algebraic.charAt(1);
        int xIndex = algebraic.indexOf('x');
        int toX = algebraic.charAt(xIndex + 1) - 'A';
        int toY = 8 - (algebraic.charAt(xIndex + 2) - '0');

        ChessPiece movedPiece = findPieceCanMoveTo(pieceChar, toX, toY, whiteTurn, board, disambiguator);

        if (movedPiece == null) return new ChessMove(-1, -1, -1, -1, board, algebraic);

        return new ChessMove(movedPiece.getX(), movedPiece.getY(), toX, toY, board, algebraic);
    }

    @Contract("_, _, _ -> new")
    private static @NotNull ChessMove castling(boolean kingside, boolean whiteTurn, @NotNull ChessBoard board) {
        int rank = whiteTurn ? 7 : 0;
        int fromX = 4;
        int toX = kingside ? 6 : 2;

        return new ChessMove(fromX, rank, toX, rank, board, false, '\0', true, false, kingside ? "0-0" : "0-0-0");
    }

    private static @Nullable ChessPiece findPieceCanMoveTo(char pieceChar, int toX, int toY, boolean whiteTurn,
                                                           @NotNull ChessBoard board, Character disambiguator) {
        List<ChessPiece> pieces = board.getPieces(whiteTurn).stream()
                .filter(piece -> piece.getChar() == pieceChar)
                .toList();

        List<ChessPiece> validPieces = pieces.stream()
                .filter(piece -> Arrays.stream(piece.getValidFields())
                        .anyMatch(field -> field[0] == toX && field[1] == toY))
                .toList();

        if (validPieces.size() == 1) {
            return validPieces.getFirst();
        }

        if (disambiguator != null && validPieces.size() > 1) {
            if (disambiguator >= 'A' && disambiguator <= 'H') {
                int fileX = disambiguator - 'A';
                return validPieces.stream()
                        .filter(piece -> piece.getX() == fileX)
                        .findFirst()
                        .orElse(null);
            }

            if (disambiguator >= '1' && disambiguator <= '8') {
                int rankY = 8 - (disambiguator - '0');
                return validPieces.stream()
                        .filter(piece -> piece.getY() == rankY)
                        .findFirst()
                        .orElse(null);
            }
        }

        return validPieces.isEmpty() ? null : validPieces.getFirst();
    }
}