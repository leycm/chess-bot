package org.leycm.chessbot.chess.pieces;

import org.leycm.chessbot.chess.ChessBoard;
import org.leycm.chessbot.chess.Piece;
import org.leycm.chessbot.util.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

public class KnightChessPiece extends Piece {

    public KnightChessPiece(boolean isWhite, ChessBoard board) {
        super(isWhite, board, 3, "knight_chess_piece", "Knight", '♞');
    }

    @Override
    public boolean isValidMove(int targetX, int targetY) {
        return ArrayUtils.containsInArray(new int[]{targetX, targetY}, getValidFields());
    }

    @Override
    public int[][] getValidFields() {
        List<int[]> fields = new ArrayList<>();
        int currentX = getX();
        int currentY = getY();

        int[][] knightMoves = {
                {+2, +1}, {+2, -1}, {-2, +1}, {-2, -1},
                {+1, +2}, {+1, -2}, {-1, +2}, {-1, -2}
        };

        for (int[] move : knightMoves) {
            int newX = currentX + move[0];
            int newY = currentY + move[1];

            if (canMoveTo(newX, newY)) {
                fields.add(new int[]{newX, newY});
            }
        }

        return fields.toArray(new int[][]{});
    }

    public boolean isFieldValid(int targetX, int targetY) {
        return canMoveTo(targetX, targetY);
    }

    public List<int[]> getPossibleFields(int offsetX, int offsetY) {
        List<int[]> possibleFields = new ArrayList<>();
        int currentX = getX();
        int currentY = getY();

        if (Math.abs(offsetX) == 1 && offsetY == 0) {
            possibleFields.add(new int[]{currentX + offsetX * 2, currentY + 1});
            possibleFields.add(new int[]{currentX + offsetX * 2, currentY - 1});
        } else if (offsetX == 0 && Math.abs(offsetY) == 1) {
            possibleFields.add(new int[]{currentX + 1, currentY + offsetY * 2});
            possibleFields.add(new int[]{currentX - 1, currentY + offsetY * 2});
        }

        return possibleFields;
    }
}
