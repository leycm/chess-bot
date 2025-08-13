package org.leycm.chessbot.chess.pieces;

import org.jetbrains.annotations.NotNull;
import org.leycm.chessbot.chess.ChessBoard;
import org.leycm.chessbot.chess.Piece;
import org.leycm.chessbot.util.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

public class RookChessPiece extends Piece {

    public RookChessPiece(boolean isWhite, ChessBoard board) {
        super(isWhite, board, 5, "rook_chess_piece", "Rook", '♜');
    }

    @Override
    public boolean isValidMove(int targetX, int targetY) {
        return ArrayUtils.containsInArray(new int[]{targetX, targetY}, getValidFields());
    }

    @Override
    public int[][] getValidFields() {
        List<int[]> fields = new ArrayList<>();

        fields.addAll(getMovesInDirection(+1, +0));
        fields.addAll(getMovesInDirection(-1, +0));
        fields.addAll(getMovesInDirection(+0, +1));
        fields.addAll(getMovesInDirection(+0, -1));

        return fields.toArray(new int[0][]);
    }

    private @NotNull List<int[]> getMovesInDirection(int deltaX, int deltaY) {
        List<int[]> moves = new ArrayList<>();
        int currentX = getX();
        int currentY = getY();

        for (int i = 1; i < 8; i++) {
            int newX = currentX + deltaX * i;
            int newY = currentY + deltaY * i;

            if (!isValidCoordinate(newX, newY)) {
                break;
            }

            if (isEmpty(newX, newY)) {
                moves.add(new int[]{newX, newY});
            } else if (isEnemy(newX, newY)) {
                moves.add(new int[]{newX, newY});
                break;
            } else {
                break;
            }
        }

        return moves;
    }
}