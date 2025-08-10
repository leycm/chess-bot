package org.leycm.chessbot.chess;

public abstract class Piece {

    protected int x;
    protected int y;

    protected final boolean isWhite;
    protected final ChessBoard board;

    protected final int level;

    protected final String id;
    protected final String name;
    protected final char ico;

    public Piece(boolean isWhite, ChessBoard board, int level, String id, String name, char ico) {
        this.isWhite = isWhite;
        this.board = board;
        this.level = level;
        this.id = id;
        this.name = name;
        this.ico = ico;
    }

    public abstract boolean isValidMove(int targetX, int targetY);

    public abstract Object[] getValidFields();

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

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public char getIco() {
        return ico;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

}

