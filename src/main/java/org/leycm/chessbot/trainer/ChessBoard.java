package org.leycm.chessbot.trainer;

public class ChessBoard {
    private final int[] board = new int[64];
    private boolean whiteToMove = true;

    public ChessBoard() {
        setupInitialPosition();
    }

    private void setupInitialPosition() {
        // White
        board[0] = 4; board[1] = 2; board[2] = 3; board[3] = 5;
        board[4] = 6; board[5] = 3; board[6] = 2; board[7] = 4;
        for (int i = 8; i < 16; i++) board[i] = 1;

        // Empty
        for (int i = 16; i < 48; i++) board[i] = 0;

        // Black
        for (int i = 48; i < 56; i++) board[i] = 11;
        board[56] = 14; board[57] = 12; board[58] = 13; board[59] = 15;
        board[60] = 16; board[61] = 13; board[62] = 12; board[63] = 14;
    }

    public int[] toArray() {
        return board.clone();
    }

    public boolean isWhiteToMove() {
        return whiteToMove;
    }

    public void applyMove(PGNParser.Move move) {
        int from = move.from();
        int to = move.to();

        board[to] = board[from];
        board[from] = 0;
        whiteToMove = !whiteToMove;
    }

    public static int[] getCurrentBoard() {
        return new ChessBoard().toArray();
    }
}