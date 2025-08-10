package org.leycm.chessbot.chess.pieces;

import org.jetbrains.annotations.NotNull;
import org.leycm.chessbot.chess.ChessBoard;
import org.leycm.chessbot.chess.Piece;
import org.leycm.chessbot.util.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

public class KingChessPiece extends Piece {
    public KingChessPiece(boolean isWhite, ChessBoard board) {
        super(isWhite, board, 3, "king_chess_piece", "King", '.');
    }

    @Override
    public boolean isValidMove(int targetX, int targetY) {
        return ArrayUtils.containsInArray(new int[]{targetX, targetY}, getValidFields());
    }

    @Override
    public int[][] getValidFields() {

        List<int[]> fields = new ArrayList<>();

        List<int[]> possibleFields = new ArrayList<>();
        possibleFields.add(new int[]{getX() + 1, getY() + 1});
        possibleFields.add(new int[]{getX() + 0, getY() + 1});
        possibleFields.add(new int[]{getX() + -1, getY() + 1});
        possibleFields.add(new int[]{getX() + 1, getY() + 0});
        possibleFields.add(new int[]{getX() + 1, getY() + -1});
        possibleFields.add(new int[]{getX() + 0, getY() + -1});
        possibleFields.add(new int[]{getX() + -1, getY() + -1});
        possibleFields.add(new int[]{getX() + -1, getY() + 0});
        possibleFields.forEach(ints -> {
            if (isFreeSpot(ints[0], ints[1]) || this.isWhite != this.board.getPiece(ints[0], ints[1]).isWhite()) {

            }
        });

        return fields.toArray(new int[][]{});
    }

    private boolean isFreeSpot(int targetX, int targetY) {
        return this.board.getPiece(targetX, targetY) == null;
    }
}
