package org.leycm.chessbot.chess;

import lombok.Data;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.leycm.chessbot.chess.pieces.PawnChessPiece;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

@Data
public class ChessMove implements Serializable {
    private final ChessPiece movedPiece;
    private final ChessPiece capturedPiece;
    private final int fromX, fromY;
    private final int toX, toY;
    private final boolean isPromotion;
    private final char promotionPiece;
    private final boolean isCastling;
    private final boolean isEnPassant;

    private final ChessBoard board;

    public ChessMove(int fromX, int fromY, int toX, int toY, @NotNull ChessBoard board) {
        this(fromX, fromY, toX, toY, board, false, '\0', false, false);
    }

    public ChessMove(int fromX, int fromY, int toX, int toY, @NotNull ChessBoard board,
                     boolean isPromotion, char promotionPiece, boolean isCastling, boolean isEnPassant) {
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

    public boolean isValid() {
        return fromX >= 0 &&
                fromY >= 0 &&
                toX >= 0 &&
                toY >= 0;
    }

    public String asCoordinate() {
        return (char)(fromX + 'A') +
                (8 - fromY) + "-" +
                (char)(toX + 'A') +
                (8 - toY);
    }

    public String asFigurineAlgebraic() {
        if (fromX == -1 || fromY == -1 || toX == -1 || toY == -1) return "";

        if (isCastling) return toX == 6 ? "O-O" : "O-O-O";

        StringBuilder result = new StringBuilder();

        if (movedPiece != null && !(movedPiece instanceof PawnChessPiece))
            result.append(getPieceFigurine(movedPiece.getChar(), movedPiece.isWhite()));

        String disambiguation = getDisambiguation();
        result.append(disambiguation);

        if (capturedPiece != null || isEnPassant) {
            if (movedPiece instanceof PawnChessPiece && disambiguation.isEmpty())
                result.append((char)('a' + fromX));

            result.append("x");
        }

        result.append((char)('a' + toX));
        result.append(8 - toY);

        if (isPromotion && movedPiece != null) {
            result.append("=");
            result.append(getPieceFigurine(promotionPiece, movedPiece.isWhite()));
        }

        return result.toString();
    }

    @Contract(pure = true)
    private @NotNull String getPieceFigurine(char pieceChar, boolean isWhite) {
        return switch (pieceChar) {
            case 'K' -> isWhite ? "♔" : "♚";
            case 'Q' -> isWhite ? "♕" : "♛";
            case 'R' -> isWhite ? "♖" : "♜";
            case 'B' -> isWhite ? "♗" : "♝";
            case 'N' -> isWhite ? "♘" : "♞";
            default -> ""; 
        };
    }

    private @NotNull String getDisambiguation() {
        if (movedPiece == null || movedPiece instanceof PawnChessPiece) {
            return "";
        }

        List<ChessPiece> samePieces = board.getPieces(movedPiece.isWhite()).stream()
                .filter(piece -> piece.getChar() == movedPiece.getChar())
                .filter(piece -> piece.getX() != fromX || piece.getY() != fromY)
                .filter(piece -> Arrays.stream(piece.getValidFields())
                        .anyMatch(field -> field[0] == toX && field[1] == toY))
                .toList();

        if (samePieces.isEmpty()) {
            return "";
        }

        boolean fileUnique = samePieces.stream()
                .noneMatch(piece -> piece.getX() == fromX);

        if (fileUnique) {
            return String.valueOf((char)('a' + fromX));
        }

        boolean rankUnique = samePieces.stream()
                .noneMatch(piece -> piece.getY() == fromY);

        if (rankUnique) {
            return String.valueOf(8 - fromY);
        }

        return String.valueOf((char)('a' + fromX)) + (8 - fromY);
    }

    @Contract("_, _, _ -> new")
    private static @NotNull ChessMove castling(boolean kingside, boolean whiteTurn, @NotNull ChessBoard board) {
        int rank = whiteTurn ? 0 : 7;
        int fromX = 4;
        int toX = kingside ? 6 : 2;

        return new ChessMove(fromX, rank, toX, rank, board, false, '\0', true, false);
    }


}