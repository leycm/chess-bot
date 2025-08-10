package org.leycm.chessbot.chess.pieces;

import org.jetbrains.annotations.NotNull;
import org.leycm.chessbot.chess.ChessBoard;
import org.leycm.chessbot.chess.Piece;
import org.leycm.chessbot.util.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

public class QueenChessPiece extends Piece {
    public QueenChessPiece(boolean isWhite, ChessBoard board) {
        super(isWhite, board, 3, "queen_chess_piece", "Queen", '.');
    }

    @Override
    public boolean isValidMove(int targetX, int targetY) {
        return ArrayUtils.containsInArray(new int[]{targetX, targetY}, getValidFields());
    }

    @Override
    public Object[] getValidFields() {

        List<int[]> fields = new ArrayList<>();

        checkInDirection(1, 1);
        checkInDirection(1, -1);
        checkInDirection(-1, 1);
        checkInDirection(-1, -1);

        checkInDirection(1, 0);
        checkInDirection(0, 1);
        checkInDirection(-1, 0);
        checkInDirection(0, -1);

        return fields.toArray(new int[][]{});
    }

    private @NotNull List<int[]> checkInDirection(int offsetX, int offsetY) {

        List<int[]> fieldsInDirection = new ArrayList<>();

        boolean isCollided = false;
        int repeated = 0;

        while ( !isCollided) {

            repeated++;
            int checkingX = x + offsetX * repeated;
            int checkingY = y + offsetY * repeated;

            if (checkingX > 8 || checkingX < 0 || checkingY > 8 || checkingY < 0) {
                isCollided = true;
                break;
            }

            if (!isFreeSpot(checkingX, checkingY)) {
                isCollided = true;
                if (this.isWhite != this.board.getPiece(checkingX, checkingY).isWhite()) {
                    fieldsInDirection.add(new int[]{checkingX, checkingY});
                }
            } else {
                fieldsInDirection.add(new int[]{checkingX, checkingY});
            }

        }

        return fieldsInDirection;
    }

    private boolean isFreeSpot(int targetX, int targetY) {
        return this.board.getPiece(targetX, targetY) == null;
    }
}
