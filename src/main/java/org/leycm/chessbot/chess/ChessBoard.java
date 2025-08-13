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
        if (!isValidCoord(fromX, fromY) || !isValidCoord(toX, toY))
            return;

        Piece piece = board[fromY][fromX];
        if (piece == null || !piece.isValidMove(toX, toY))
            return;

        Piece captured = board[toY][toX];

        board[toY][toX] = piece;
        board[fromY][fromX] = null;

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
        //noinspection SuspiciousToArrayCall
        return Arrays.stream(board)
                .map(Objects::isNull)
                .toList().toArray(new boolean[8][8]);
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

