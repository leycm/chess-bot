package org.leycm.chessbot.chess.pieces;

import org.leycm.chessbot.chess.ChessBoard;
import org.leycm.chessbot.chess.Piece;

import java.util.ArrayList;
import java.util.List;

import static org.leycm.chessbot.utils.IntArray.*;

public class TowerChessPiece extends Piece {
    public TowerChessPiece(boolean isWhite, ChessBoard board) {
        super(isWhite, board, 4, "tower_chess_piece", "Tower", '.');
    }

    @Override
    public boolean isValidMove(int targetX, int targetY) {
        return containsIntArray(getValidMove(), new int[]{targetX, targetY});
    }

    @Override
    public int[][] getValidMove() {

        int[][] returnValue = new int[][]{};

        for (int[] ints : checkInDirection(1, 0, x, y)) {
            addIntArrayToIntArraysOfArray(returnValue, ints);
        }

        for (int[] ints : checkInDirection(0, 1, x, y)) {
            addIntArrayToIntArraysOfArray(returnValue, ints);
        }

        for (int[] ints : checkInDirection(-1, 0, x, y)) {
            addIntArrayToIntArraysOfArray(returnValue, ints);
        }

        for (int[] ints : checkInDirection(0, -1, x, y)) {
            addIntArrayToIntArraysOfArray(returnValue, ints);
        }

        return returnValue;
    }

    private int[][] checkInDirection(int offsetX, int offsetY, int currentX, int currentY) {

        int[][] returnValue = new int[][]{};

        boolean isCollided = false;
        int repeated = 0;
        while ( ! (isCollided)) {
            repeated++;
            if ( ! (isFreeSpot(currentX + offsetX * repeated, currentY + offsetY * repeated))) {
                isCollided = true;
                if (this.isWhite != this.board.getPiece(currentX + offsetX * repeated, currentY + offsetY * repeated).isWhite()) {
                    addIntArrayToIntArraysOfArray(returnValue, new int[]{currentX + offsetX * repeated, currentY + offsetY * repeated});
                }
            } else {
                addIntArrayToIntArraysOfArray(returnValue, new int[]{currentX + offsetX * repeated, currentY + offsetY * repeated});
            }
        }
        return returnValue;
    }

    private boolean isFreeSpot(int targetX, int targetY) {
        return this.board.getPiece(targetX, targetY) == null;
    }
}
