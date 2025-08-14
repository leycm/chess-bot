package org.leycm.chessbot.chess.pieces;

import org.leycm.chessbot.chess.ChessBoard;
import org.leycm.chessbot.chess.ChessPiece;
import org.leycm.chessbot.util.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

public class KingChessPiece extends ChessPiece {

    public KingChessPiece(boolean isWhite, ChessBoard board) {
        super(isWhite, board, 1000, "king_chess_piece", "King", '♚'); 
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

        int[][] directions = {
                {-1, -1}, {-1, +0}, {-1, +1},
                {+0, -1},           {+0, +1},
                {+1, -1}, {+1, +0}, {+1, +1}
        };

        for (int[] direction : directions) {
            int newX = currentX + direction[0];
            int newY = currentY + direction[1];

            if (canMoveTo(newX, newY)) {
                fields.add(new int[]{newX, newY});
            }
        }

        return fields.toArray(new int[0][]);
    }
}