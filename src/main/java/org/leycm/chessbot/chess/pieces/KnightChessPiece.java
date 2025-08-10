package org.leycm.chessbot.chess.pieces;

import org.leycm.chessbot.chess.ChessBoard;
import org.leycm.chessbot.chess.Piece;
import org.leycm.chessbot.util.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class KnightChessPiece extends Piece {
    public KnightChessPiece(boolean isWhite, ChessBoard board) {
        super(isWhite, board, 2, "knight_chess_piece", "Knight", '.');
    }

    @Override
    public boolean isValidMove(int targetX, int targetY) {
        return ArrayUtils.containsInArray(new int[]{targetX, targetY}, getValidFields());
    }

    @Override
    public Object[] getValidFields() {

        List<int[]> fields = new ArrayList<>();

        getPossibleFields(1, 0).forEach(ints -> {
            if (isFieldValid(ints[0], ints[1])) {
                fields.add(ints);
            }
        });
        getPossibleFields(0, 1).forEach(ints -> {
            if (isFieldValid(ints[0], ints[1])) {
                fields.add(ints);
            }
        });
        getPossibleFields(-1, 0).forEach(ints -> {
            if (isFieldValid(ints[0], ints[1])) {
                fields.add(ints);
            }
        });
        getPossibleFields(0, -1).forEach(ints -> {
            if (isFieldValid(ints[0], ints[1])) {
                fields.add(ints);
            }
        });

        return fields.toArray(new int[][]{});
    }

    public boolean isFieldValid(int targetX, int targetY) {

        Piece checkingPiece = this.board.getPiece(targetX, targetY);
        return checkingPiece == null || this.isWhite != checkingPiece.isWhite();
    }

    public List<int[]> getPossibleFields(int offsetX, int offsetY) {

        List<int[]> possibleFields = new ArrayList<>();

        offsetX = Math.clamp(offsetX, -1, 1);
        offsetY = Math.clamp(offsetY, -1, 1);

        if (offsetX != 0 && offsetY == 0) {
            possibleFields.add(new int[]{x + offsetY * 3, y + 1});
            possibleFields.add(new int[]{x + offsetY * 3, y - 1});
        } else if (offsetX == 0 && offsetY != 0) {
            possibleFields.add(new int[]{x + 1, y + offsetX * 3});
            possibleFields.add(new int[]{x - 1, y + offsetX * 3});
        }

        return possibleFields;
    }
}
