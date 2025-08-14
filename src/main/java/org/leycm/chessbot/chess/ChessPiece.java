package org.leycm.chessbot.chess;

import lombok.Data;

import java.util.UUID;

@Data
public abstract class ChessPiece {

    protected final boolean isWhite;
    public boolean hasMovedYet = false;
    protected final ChessBoard board;
    protected final UUID uuid;
    protected final int level;
    protected final String id;
    protected final String name;
    protected final char ico;

    public ChessPiece(boolean isWhite, ChessBoard board, int level, String id, String name, char ico) {
        this.isWhite = isWhite;
        this.board = board;
        this.level = level;
        this.id = id;
        this.name = name;
        this.ico = ico;
        this.uuid = UUID.randomUUID();
    }

    public abstract boolean isValidMove(int targetX, int targetY);

    public abstract int[][] getValidFields();

    public int getX() {
        return board.getXForPiece(uuid);
    }

    public int getY() {
        return board.getYForPiece(uuid);
    }

    public char getColorChar() {
        return isWhite ? Character.toLowerCase(getChar()) : Character.toUpperCase(getChar());
    }

    public char getChar() {
        return Character.toUpperCase(name.charAt(0));
    }

    protected boolean isValidCoordinate(int x, int y) {
        return board.isValidCoord(x, y);
    }

    protected boolean isEmpty(int x, int y) {
        return board.getPiece(x, y) == null;
    }

    protected boolean isEnemy(int x, int y) {
        ChessPiece piece = board.getPiece(x, y);
        return piece != null && piece.isWhite() != this.isWhite();
    }

    protected boolean canMoveTo(int x, int y) {
        return isValidCoordinate(x, y) && (isEmpty(x, y) || isEnemy(x, y));
    }
}