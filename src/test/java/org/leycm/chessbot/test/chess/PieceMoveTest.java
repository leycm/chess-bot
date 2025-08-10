package org.leycm.chessbot.test.chess;

import org.leycm.chessbot.chess.ChessBoard;
import org.leycm.chessbot.chess.Piece;
import org.leycm.chessbot.chess.pieces.*;

public class PieceMoveTest {

    public static void main(String[] args) {

        ChessBoard chessBoard = new ChessBoard();

        testChessPiece(chessBoard, new PawnChessPiece(true, chessBoard), 3, 3, new int[][]{{1, 1}, {0, 3}, {6, 6}, {0, 5}, {2, 2}}, new int[][]{{3, 4}});
        testChessPiece(chessBoard, new KnightChessPiece(true, chessBoard), 3, 3, new int[][]{{1, 1}, {0, 3}, {6, 6}, {0, 5}, {2, 2}}, new int[][]{{3, 4}});
        testChessPiece(chessBoard, new BishopChessPiece(true, chessBoard), 3, 3, new int[][]{{1, 1}, {0, 3}, {6, 6}, {0, 5}, {2, 2}}, new int[][]{{3, 4}});
        testChessPiece(chessBoard, new RookChessPiece(true, chessBoard), 3, 3, new int[][]{{1, 1}, {0, 3}, {6, 6}, {0, 5}, {2, 2}}, new int[][]{{3, 4}});
        testChessPiece(chessBoard, new QueenChessPiece(true, chessBoard), 3, 3, new int[][]{{1, 1}, {0, 3}, {6, 6}, {0, 5}, {2, 2}}, new int[][]{{3, 4}});
        testChessPiece(chessBoard, new KingChessPiece(true, chessBoard), 3, 3, new int[][]{{1, 1}, {0, 3}, {6, 6}, {0, 5}, {2, 2}}, new int[][]{{3, 4}});

    }

    public static void testChessPiece(ChessBoard chessBoard, Piece piece, int x, int y, int[][] obstacle, int[][] teammates) {

        chessBoard.placePiece(piece, x, y);

        System.out.println(" " + piece.getName());

        for (int[] ints : obstacle) {
            chessBoard.placePiece(new RookChessPiece(false, chessBoard), ints[0], ints[1]);
        }

        for (int[] ints : teammates) {
            chessBoard.placePiece(new RookChessPiece(true, chessBoard), ints[0], ints[1]);
        }

        int[][] validFields = chessBoard.getPiece(x, y).getValidFields();
        for (int[] validField : validFields) {
            System.out.println("  " + validField[0] + " | " + validField[1]);
        }
        System.out.println("\n");

    }

}
