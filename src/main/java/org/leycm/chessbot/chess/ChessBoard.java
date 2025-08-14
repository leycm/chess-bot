package org.leycm.chessbot.chess;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ChessBoard {

    private final ChessPiece[][] board = new ChessPiece[8][8];
    private final List<ChessMove> moveHistory = new ArrayList<>();

    @Setter @Getter
    private boolean whiteTurn;

    public ChessBoard() {
        this.whiteTurn = true;
    }

    public void placePiece(ChessPiece piece, int x, int y) {
        if (isValidCoord(x, y)) {
            board[y][x] = piece;
        }
    }

    public void movePiece(int fromX, int fromY, int toX, int toY) {
        movePiece(new ChessMove(fromX, fromY, toX, toY, this));
    }

    public void movePiece(@NotNull ChessMove move) {
        if (!isValidCoord(move.getFromX(), move.getFromY()) || !isValidCoord(move.getToX(), move.getToY())) {
            System.out.println("The coordinate is outside the board");
            return;
        }

        ChessPiece piece = board[move.getFromY()][move.getFromX()];
        if (piece == null) return;

        if (!piece.isValidMove(move.getToX(), move.getToY())) return;


        board[move.getToY()][move.getToX()] = piece;
        board[move.getFromY()][move.getFromX()] = null;

        moveHistory.add(move);

        piece.hasMovedYet = true;

        whiteTurn = !whiteTurn;
    }

    public ChessPiece getPiece(int x, int y) {
        if (!isValidCoord(x, y)) return null;
        return board[y][x];
    }

    public int getXForPiece(UUID uuid) {
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                ChessPiece piece = board[y][x];
                if (piece != null && piece.uuid.equals(uuid)) {
                    return x;
                }
            }
        }
        return -1; 
    }

    public int getYForPiece(UUID uuid) {
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                ChessPiece piece = board[y][x];
                if (piece != null && piece.uuid.equals(uuid)) {
                    return y;
                }
            }
        }
        return -1; 
    }

    public boolean isValidCoord(int x, int y) {
        return x >= 0 && x < 8 && y >= 0 && y < 8;
    }

    public List<ChessMove> getMoveHistory() {
        return new ArrayList<>(moveHistory);
    }

    public ChessPiece[][] getPieceBoard() {
        return board;
    }

    public int[][] getLevelBoard() {
        int[][] result = new int[8][8];
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                ChessPiece piece = board[y][x];
                result[y][x] = piece == null ? 0 : piece.getLevel() * (piece.isWhite() ? 1 : -1);
            }
        }
        return result;
    }

    public boolean[][] getNotNullBoard() {
        boolean[][] result = new boolean[8][8];
        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[row].length; col++) {
                result[row][col] = board[row][col] != null;
            }
        }
        return result;
    }

    public ChessPiece[] getPieceArray() {
        return Arrays.stream(board)
                .flatMap(Arrays::stream)
                .toArray(ChessPiece[]::new);
    }

    public int[] getLevelArray() {
        return Arrays.stream(board)
                .flatMapToInt(row -> Arrays.stream(row)
                        .mapToInt(p -> p == null ? 0 : p.getLevel() * (p.isWhite() ? 1 : -1)))
                .toArray();
    }

    public int[] getGameStateArray() {
            int[] result = new int[65];
            int[] boardValues = Arrays.stream(board)
                    .flatMapToInt(row -> Arrays.stream(Arrays.stream(row)
                            .mapToInt(p -> p == null ? 0 : p.getLevel() * (p.isWhite() ? 1 : -1))
                            .toArray())).toArray();
            System.arraycopy(boardValues, 0, result, 0, boardValues.length);
            result[64] = whiteTurn ? 1 : 0;
            return result;
    }

    public List<ChessPiece> getPieces(boolean white) {
        List<ChessPiece> pieces = new ArrayList<>();
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                ChessPiece piece = board[y][x];
                if (piece != null && piece.isWhite() == white) {
                    pieces.add(piece);
                }
            }
        }
        return pieces;
    }

    public String toVisualString() {
        StringBuilder sb = new StringBuilder();

        for (int y = 0; y < 8; y++) {
            char rowLabel = (char) ('1' + (7 - y));
            sb.append(rowLabel).append("  ");

            for (int x = 0; x < 8; x++) {
                ChessPiece piece = board[y][x];
                sb.append(piece != null ? piece.getColorChar() : '.').append("  ");
            }
            sb.append("\n");
        }

        sb.append("   ");
        for (int x = 0; x < 8; x++) {
            char colLabel = (char) ('a' + x);
            sb.append(colLabel).append("  ");
        }
        sb.append("\n");

        return sb.toString();
    }

}
