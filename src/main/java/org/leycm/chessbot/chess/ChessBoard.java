package org.leycm.chessbot.chess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ChessBoard {

    private final Piece[][] board = new Piece[8][8];
    private final List<Move> moveHistory = new ArrayList<>();

    public ChessBoard() {}

    public void placePiece(Piece piece, int x, int y) {
        if (isValidCoord(x, y)) {
            board[y][x] = piece;
        }
    }

    public boolean movePiece(int fromX, int fromY, int toX, int toY) {
        if (!isValidCoord(fromX, fromY) || !isValidCoord(toX, toY))
            return false;

        Piece piece = board[fromY][fromX];
        if (piece == null || !piece.isValidMove(toX, toY))
            return false;

        Piece captured = board[toY][toX];

        board[toY][toX] = piece;
        board[fromY][fromX] = null;

        moveHistory.add(new Move(fromX, fromY, toX, toY, piece, captured));
        return true;
    }



    public Piece getPiece(int x, int y) {
        if (!isValidCoord(x, y)) return null;
        return board[y][x];
    }

    public int getXForPiece(UUID uuid) {
        for (int pieceX = 0; pieceX < 8; pieceX++) {
            for (int pieceY = 0; pieceY < 8; pieceY++) {
                if (board[pieceX][pieceY].uuid.equals(uuid)) return pieceX;
            }
        }

        return 0;
    }

    public int getYForPiece(UUID uuid) {
        for (int pieceX = 0; pieceX < 8; pieceX++) {
            for (int pieceY = 0; pieceY < 8; pieceY++) {
                if (board[pieceX][pieceY].uuid.equals(uuid)) return pieceY;
            }
        }

        return 0;
    }

    public boolean isValidCoord(int x, int y) {
        return x >= 0 && x < 8 && y >= 0 && y < 8;
    }

    public List<Move> getMoveHistory() {
        return moveHistory;
    }

    public Piece[][] getBoard() {
        return board;
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


    public record Move(int fromX, int fromY, int toX, int toY, Piece movedPiece, Piece capturedPiece) {}

}

