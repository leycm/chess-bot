package org.leycm.chessbot.chess;

import java.util.UUID;

public abstract class Piece {

    protected final boolean isWhite;
    protected final ChessBoard board;
    protected final UUID uuid;

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
        this.uuid = UUID.randomUUID();
    }

    public abstract boolean isValidMove(int targetX, int targetY);

    public abstract int[][] getValidFields();

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
        return board.getXForPiece(uuid);
    }

    public int getY() {
        return board.getYForPiece(uuid);
    }

}

