package org.leycm.chessbot.chess.pieces;

import org.leycm.chessbot.chess.ChessBoard;
import org.leycm.chessbot.chess.ChessPiece;
import org.leycm.chessbot.util.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

public class PawnChessPiece extends ChessPiece {

    public PawnChessPiece(boolean isWhite, ChessBoard board) {
        super(isWhite, board, 1, "pawn_chess_piece", "Pawn", '♟');
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

        int direction = isWhite ? -1 : 1;

        int oneStepY = currentY + direction;
        if (isValidCoordinate(currentX, oneStepY) && isEmpty(currentX, oneStepY)) {
            fields.add(new int[]{currentX, oneStepY});


            int twoStepY = currentY + (direction * 2);
            if (!hasMovedYet && isValidCoordinate(currentX, twoStepY) && isEmpty(currentX, twoStepY)) {
                fields.add(new int[]{currentX, twoStepY});
            }
        }

        for (int deltaX : new int[]{1, -1}) {
            int captureX = currentX + deltaX;
            int captureY = currentY + direction;

            if (isValidCoordinate(captureX, captureY) && isEnemy(captureX, captureY)) {
                fields.add(new int[]{captureX, captureY});
            }
        }

        return fields.toArray(new int[0][]);
    }
}
