package org.leycm.chessbot.chess;

import java.util.ArrayList;
import java.util.List;

public class ChessBoard {

    private final Piece[][] board = new Piece[8][8];
    private final List<Move> moveHistory = new ArrayList<>();

    public ChessBoard() {}

    public void placePiece(Piece piece, int x, int y) {
        if (isValidCoord(x, y)) {
            board[y][x] = piece;
            piece.setPosition(x, y);
        }
    }

    public boolean movePiece(int fromX, int fromY, int toX, int toY) {
        if (!isValidCoord(fromX, fromY) || !isValidCoord(toX, toY))
            return false;

        Piece piece = board[fromY][fromX];
        if (piece == null || !piece.isValidMove(toX, toY, this))
            return false;

        Piece captured = board[toY][toX];
        board[toY][toX] = piece;
        board[fromY][fromX] = null;
        piece.setPosition(toX, toY);

        moveHistory.add(new Move(fromX, fromY, toX, toY, piece, captured));
        return true;
    }

    public Piece getPiece(int x, int y) {
        if (!isValidCoord(x, y)) return null;
        return board[y][x];
    }

    public boolean isValidCoord(int x, int y) {
        return x >= 0 && x < 8 && y >= 0 && y < 8;
    }

    public List<Move> getMoveHistory() {
        return moveHistory;
    }

    public static class Move {
        public final int fromX, fromY, toX, toY;
        public final Piece movedPiece;
        public final Piece capturedPiece;

        public Move(int fromX, int fromY, int toX, int toY, Piece movedPiece, Piece capturedPiece) {
            this.fromX = fromX;
            this.fromY = fromY;
            this.toX = toX;
            this.toY = toY;
            this.movedPiece = movedPiece;
            this.capturedPiece = capturedPiece;
        }

        @Override
        public String toString() {
            return movedPiece.getClass().getSimpleName() + " from (" + fromX + "," + fromY + ") to (" + toX + "," + toY + ")" +
                    (capturedPiece != null ? " capturing " + capturedPiece.getClass().getSimpleName() : "");
        }
    }
}

