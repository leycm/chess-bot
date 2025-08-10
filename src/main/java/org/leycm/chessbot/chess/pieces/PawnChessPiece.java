package org.leycm.chessbot.chess.pieces;

import org.jetbrains.annotations.NotNull;
import org.leycm.chessbot.chess.ChessBoard;
import org.leycm.chessbot.chess.Piece;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PawnChessPiece extends Piece {


    public PawnChessPiece(boolean isWhite, ChessBoard board) {
        super(isWhite, board, 1, "pawn_chess_piece", "Pawn", '♙');
    }

    @Override
    public boolean isValidMove(int targetX, int targetY) {
        int direction = isWhite ? -1 : 1;

        if (targetX == x && targetY == y + direction && board.getPiece(targetX, targetY) == null) {
            return true;
        }

        if (targetX == x && targetY == y + 2 * direction && ((isWhite && y == 6) || (!isWhite && y == 1))
                && board.getPiece(targetX, targetY) == null && board.getPiece(targetX, y + direction) == null) {
            return true;
        }

        if (Math.abs(targetX - x) == 1 && targetY == y + direction) {
            Piece target = board.getPiece(targetX, targetY);
            return target != null && target.isWhite() != this.isWhite();
        }

        return false;
    }

    @Override
    public int[][] getValidFields() {

        List<int[]> fields = new ArrayList<>();
        
        int direction = isWhite ? -1 : 1;
        int checkingY = y + direction;

        for (int i = -1; i < 2; i++) {

            int checkingX = x + i;

            if (!(checkingX > 8 || checkingX < 0 || checkingY > 8 || checkingY < 0)) {
                fields.add(new int[]{x,y});
            }

        }

        return fields.toArray(new int[][]{});
    }
}

