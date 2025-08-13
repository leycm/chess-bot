package org.leycm.chessbot.chess;

import java.util.*;

public class ChessBoard {

    private final Piece[][] board = new Piece[8][8];
    private final List<Move> moveHistory = new ArrayList<>();


    private boolean whiteOnMove;

    public ChessBoard() {
        this.whiteOnMove = true;
    }

    public void placePiece(Piece piece, int x, int y) {
        if (isValidCoord(x, y)) {
            board[y][x] = piece;
        }
    }

    public void movePiece(int fromX, int fromY, int toX, int toY) {
        if (!isValidCoord(fromX, fromY) || !isValidCoord(toX, toY)) {
            System.out.println("The Cord is outside the Board");
            return;
        }

        Piece piece = board[fromY][fromX];
        if (piece == null || !piece.isValidMove(toX, toY)) {
            System.out.println("The Piece is null");
            for (boolean[] booleans : getBooleanBoard()) {
                System.out.println(Arrays.toString(booleans));
            }
            if (piece == null) {
                return;
            }
            System.out.println("The Move is: " + piece.isValidMove(toX, toY));
            return;
        }

        Piece captured = board[toY][toX];

        board[toY][toX] = piece;
        board[fromY][fromX] = null;
        System.out.println("Has Moved");

        moveHistory.add(new Move(fromX, fromY, toX, toY, piece, captured));
        piece.hasMovedJet = true;
        whiteOnMove = !whiteOnMove;
    }



    public Piece getPiece(int x, int y) {
        if (!isValidCoord(x, y)) return null;
        return board[y][x];
    }

    public int getXForPiece(UUID uuid) {
        for (int pieceX = 0; pieceX < 8; pieceX++) {
            for (int pieceY = 0; pieceY < 8; pieceY++) {
                Piece piece = board[pieceX][pieceY];
                if (piece == null) continue;
                if (piece.uuid.equals(uuid)) return pieceX;
            }
        }

        return 0;
    }

    public int getYForPiece(UUID uuid) {
        for (int pieceX = 0; pieceX < 8; pieceX++) {
            for (int pieceY = 0; pieceY < 8; pieceY++) {
                Piece piece = board[pieceX][pieceY];
                if (piece == null) continue;
                if (piece.uuid.equals(uuid)) return pieceY;
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

    public Piece[][] getPieceBoard() {
        return board;
    }

    public int[] getLevelBoard() {
        return Arrays.stream(board)
                .flatMapToInt(row -> Arrays.stream(row)
                        .mapToInt(p -> p == null ? 0 : p.getLevel() + (p.isWhite() ? 0 : 10)))
                .toArray();
    }

    public boolean[][] getBooleanBoard() {
        boolean[][] result = new boolean[8][8];
        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[row].length; col++) {
                result[row][col] = board[row][col] == null;
            }
        }
        return result;
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

    public boolean isWhiteOnMove() {
        return whiteOnMove;
    }

    public void setWhiteOnMove(boolean whiteObMove) {
        this.whiteOnMove = whiteObMove;
    }


    public record Move(int fromX, int fromY, int toX, int toY, Piece movedPiece, Piece capturedPiece) {}

}

