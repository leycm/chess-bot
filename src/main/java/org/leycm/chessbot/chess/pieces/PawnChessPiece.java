package org.leycm.chessbot.chess.pieces;

import org.jetbrains.annotations.NotNull;
import org.leycm.chessbot.chess.ChessBoard;
import org.leycm.chessbot.chess.Piece;
import org.leycm.chessbot.util.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PawnChessPiece extends Piece {


    public PawnChessPiece(boolean isWhite, ChessBoard board) {
        super(isWhite, board, 1, "pawn_chess_piece", "Pawn", '♙');
    }

    @Override
    public boolean isValidMove(int targetX, int targetY) {
        return ArrayUtils.containsInArray(new int[]{targetX, targetY}, getValidFields());
    }

    @Override
    public int[][] getValidFields() {

        List<int[]> fields = new ArrayList<>();

        int direction = isWhite ? -1 : 1;
        int checkingY = getY() + direction;

        for (int i = -1; i < 2; i++) {

            int checkingX = getX() + i;

            if (!(checkingX > 8 || checkingX < 0 || checkingY > 8 || checkingY < 0)) {
                if (i != 0) {
                    if (this.board.getPiece(checkingX, checkingY) != null && this.isWhite != this.board.getPiece(checkingX, checkingY).isWhite()) {
                        fields.add(new int[]{checkingX, checkingY});
                    }
                } else {
                    if (this.board.getPiece(checkingX, checkingY) != null) {
                        fields.add(new int[]{checkingX, checkingY});
                    }
                }
            }

        }

        return fields.toArray(new int[][]{});
    }
}

