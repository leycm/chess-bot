package org.leycm.chessbot.chess;

import java.util.UUID;

public abstract class Piece {

    protected final boolean isWhite;
    public boolean hasMovedJet = false;
    protected final ChessBoard board;
    protected final UUID uuid;

    protected final int level;

    protected final String id;
    protected final String name;
    protected final char whiteIco;
    protected final char blackIco;

    public Piece(boolean isWhite, ChessBoard board, int level, String id, String name, char whiteIco, char blackIco) {
        this.isWhite = isWhite;
        this.board = board;
        this.level = level;
        this.id = id;
        this.name = name;
        this.whiteIco = whiteIco;
        this.blackIco = blackIco;
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
        if (isWhite) {
            return whiteIco;
        } else {
            return blackIco;
        }
    }

    public int getX() {
        return board.getXForPiece(uuid);
    }

    public int getY() {
        return board.getYForPiece(uuid);
    }

}

