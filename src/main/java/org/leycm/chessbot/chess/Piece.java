package org.leycm.chessbot.chess;

public abstract class Piece {

    protected int x;
    protected int y;
    protected final boolean isWhite;
    protected final int level;

    public Piece(boolean isWhite, int level) {
        this.isWhite = isWhite;
        this.level = level;
    }

    public abstract boolean isValidMove(int targetX, int targetY, ChessBoard board);

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public boolean isWhite() {
        return isWhite;
    }

    public int getLevel() {
        return level;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

}

