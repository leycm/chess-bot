package org.leycm.chessbot.chess;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

public class ChessBoard {

    private final Piece[][] board = new Piece[8][8];
    private final List<Move> moveHistory = new ArrayList<>();

    @Setter @Getter
    private boolean whiteTurn;

    public ChessBoard() {
        this.whiteTurn = true;
    }

    public void placePiece(Piece piece, int x, int y) {
        if (isValidCoord(x, y)) {
            board[y][x] = piece;
        }
    }

    public void movePiece(int fromX, int fromY, int toX, int toY) {
        if (!isValidCoord(fromX, fromY) || !isValidCoord(toX, toY)) {
            System.out.println("The coordinate is outside the board");
            return;
        }

        Piece piece = board[fromY][fromX];
        if (piece == null) {
            System.out.println("No piece at source position");
            return;
        }

        if (!piece.isValidMove(toX, toY)) {
            System.out.println("Invalid move for piece: " + piece.getName());
            return;
        }

        Piece captured = board[toY][toX];

        // Execute the move
        board[toY][toX] = piece;
        board[fromY][fromX] = null;

        // Update move history and piece state
        moveHistory.add(new Move(fromX, fromY, toX, toY, piece, captured));
        piece.hasMovedJet = true;
        whiteTurn = !whiteTurn;

        System.out.println("Move executed: " + piece.getName() + " from (" + fromX + "," + fromY + ") to (" + toX + "," + toY + ")");
    }

    public Piece getPiece(int x, int y) {
        if (!isValidCoord(x, y)) return null;
        return board[y][x];
    }

    public int getXForPiece(UUID uuid) {
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                Piece piece = board[y][x];
                if (piece != null && piece.uuid.equals(uuid)) {
                    return x;
                }
            }
        }
        return -1; // -1 if piece not found
    }

    public int getYForPiece(UUID uuid) {
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                Piece piece = board[y][x];
                if (piece != null && piece.uuid.equals(uuid)) {
                    return y;
                }
            }
        }
        return -1; // -1 if piece not found
    }

    public boolean isValidCoord(int x, int y) {
        return x >= 0 && x < 8 && y >= 0 && y < 8;
    }

    public List<Move> getMoveHistory() {
        return new ArrayList<>(moveHistory);
    }

    public Piece[][] getPieceBoard() {
        return board;
    }

    public int[][] getLevelBoard() {
        int[][] result = new int[8][8];
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                Piece piece = board[y][x];
                result[y][x] = piece == null ? 0 : piece.getLevel() + (piece.isWhite() ? 0 : 10);
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

    public Piece[] getPieceArray() {
        return Arrays.stream(board)
                .flatMap(Arrays::stream)
                .toArray(Piece[]::new);
    }

    public int[] getLevelArray() {
        return Arrays.stream(board)
                .flatMapToInt(row -> Arrays.stream(row)
                        .mapToInt(p -> p == null ? 0 : p.getLevel() + (p.isWhite() ? 0 : 10)))
                .toArray();
    }

    public List<Piece> getPieces(boolean white) {
        List<Piece> pieces = new ArrayList<>();
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                Piece piece = board[y][x];
                if (piece != null && piece.isWhite() == white) {
                    pieces.add(piece);
                }
            }
        }
        return pieces;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        // Reihen von 8 bis 1 (oben nach unten)
        for (int y = 0; y < 8; y++) {
            char rowLabel = (char) ('h' - y); // h,g,f,...,a
            sb.append(rowLabel).append("  ");

            for (int x = 0; x < 8; x++) {
                Piece piece = board[y][x];
                sb.append(piece != null ? piece.getChar() : '.').append("  ");
            }
            sb.append("\n");
        }

        // Spaltenbeschriftung
        sb.append("   ");
        for (int x = 1; x <= 8; x++) {
            sb.append(x).append("  ");
        }
        sb.append("\n");

        return sb.toString();
    }


    public record Move(int fromX, int fromY,
                       int toX, int toY,
                       Piece movedPiece,
                       Piece capturedPiece
    ) { }


}
