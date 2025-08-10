package org.leycm.chessbot.trainer;

public class ChessBoard {
    private final int[] board = new int[64];
    private boolean whiteToMove = true;

    public ChessBoard() {
        setupInitialPosition();
    }

    private void setupInitialPosition() {
        // White pieces
        board[0] = 4; board[1] = 2; board[2] = 3; board[3] = 5;  // Rook, Knight, Bishop, Queen
        board[4] = 6; board[5] = 3; board[6] = 2; board[7] = 4;  // King, Bishop, Knight, Rook
        for (int i = 8; i < 16; i++) board[i] = 1;  // White pawns

        // Empty squares
        for (int i = 16; i < 48; i++) board[i] = 0;

        // Black pieces
        for (int i = 48; i < 56; i++) board[i] = 11;  // Black pawns
        board[56] = 14; board[57] = 12; board[58] = 13; board[59] = 15;  // Rook, Knight, Bishop, Queen
        board[60] = 16; board[61] = 13; board[62] = 12; board[63] = 14;  // King, Bishop, Knight, Rook
    }

    public int[] toArray() {
        return board.clone();
    }

    // New method: Returns board with turn information (65 elements)
    public int[] toArrayWithTurn() {
        int[] extendedBoard = new int[65];
        System.arraycopy(board, 0, extendedBoard, 0, 64);
        extendedBoard[64] = whiteToMove ? 1 : 0;  // 1 for white to move, 0 for black to move
        return extendedBoard;
    }

    public boolean isWhiteToMove() {
        return whiteToMove;
    }

    public void applyMove(PGNParser.Move move) {
        int from = move.from();
        int to = move.to();

        if (move.notation().equals("O-O")) {
            if (whiteToMove) {
                board[6] = board[4];
                board[5] = board[7];
                board[4] = 0;
                board[7] = 0;
            } else {
                board[62] = board[60];
                board[61] = board[63];
                board[60] = 0;
                board[63] = 0;
            }
        } else if (move.notation().equals("O-O-O")) {
            if (whiteToMove) {
                board[2] = board[4];
                board[3] = board[0];
                board[4] = 0;
                board[0] = 0;
            } else {
                board[58] = board[60];
                board[59] = board[56];
                board[60] = 0;
                board[56] = 0;
            }
        } else {
            board[to] = board[from];
            board[from] = 0;

            if (board[to] == 1 && to >= 56) {
                board[to] = 5;
            } else if (board[to] == 11 && to <= 7) {
                board[to] = 15;
            }
        }

        whiteToMove = !whiteToMove;
    }

    public static int[] getCurrentBoard() {
        return new ChessBoard().toArray();
    }

    public static int[] getCurrentBoardWithTurn() {
        return new ChessBoard().toArrayWithTurn();
    }

    public void setWhiteToMove(boolean whiteToMove) {
        this.whiteToMove = whiteToMove;
    }

    public int getPieceAt(int square) {
        if (square < 0 || square >= 64) return -1;
        return board[square];
    }

    public void setPieceAt(int square, int piece) {
        if (square >= 0 && square < 64) {
            board[square] = piece;
        }
    }

    public boolean isOccupied(int square) {
        return square >= 0 && square < 64 && board[square] != 0;
    }

    public boolean isWhitePiece(int square) {
        return square >= 0 && square < 64 && board[square] > 0 && board[square] <= 6;
    }

    public boolean isBlackPiece(int square) {
        return square >= 0 && square < 64 && board[square] >= 11 && board[square] <= 16;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Turn: ").append(whiteToMove ? "White" : "Black").append("\n");
        sb.append("  a b c d e f g h\n");

        for (int rank = 7; rank >= 0; rank--) {
            sb.append(rank + 1).append(" ");
            for (int file = 0; file < 8; file++) {
                int square = rank * 8 + file;
                char piece = getPieceChar(board[square]);
                sb.append(piece).append(" ");
            }
            sb.append(rank + 1).append("\n");
        }
        sb.append("  a b c d e f g h\n");
        return sb.toString();
    }

    private char getPieceChar(int piece) {
        return switch (piece) {
            case 0 -> '.';
            case 1 -> 'P';
            case 2 -> 'N';
            case 3 -> 'B';
            case 4 -> 'R';
            case 5 -> 'Q';
            case 6 -> 'K';
            case 11 -> 'p';
            case 12 -> 'n';
            case 13 -> 'b';
            case 14 -> 'r';
            case 15 -> 'q';
            case 16 -> 'k';
            default -> '?';
        };
    }
}