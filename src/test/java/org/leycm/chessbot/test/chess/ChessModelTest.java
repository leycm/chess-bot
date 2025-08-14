package org.leycm.chessbot.test.chess;

import org.leycm.chessbot.chess.ChessBoard;
import org.leycm.chessbot.model.ModelLoader;

public class ChessModelTest {
    public static void main(String[] args) throws InterruptedException {

        ChessBoard board = new ChessBoard(true);
        System.out.println(board.toVisualString());
        while (true) {
            System.out.println("[INFO]: Start New move for " + (board.isWhiteTurn() ? "WHITE" : "BLACK"));
            ModelLoader.makeBestMove(board);
            System.out.println(board.toVisualString());
        }


    }

}
