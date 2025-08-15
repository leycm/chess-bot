package org.leycm.chessbot.chess;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.leycm.chessbot.chess.controller.SensorController;
import org.leycm.chessbot.chess.controller.VirtualAiController;
import org.leycm.chessbot.chess.pieces.*;

import java.util.*;
import java.util.function.Consumer;

public class ChessBoard {

    private final ChessPiece[][] board = new ChessPiece[8][8];
    private final List<ChessMove> moveHistory = new ArrayList<>();
    private final Consumer<ChessBoard> startingOder;

    @Setter @Getter
    private boolean whiteTurn;
    @Getter private ChessBoard.State state = State.START;


    private final ChessController whiteController;
    private final ChessController blackController;

    @Getter private ChessController lastGameWinner = null;

    public enum State {
        START,
        PLAYING,
        CHECKMATE_WHITE_WINS,
        CHECKMATE_BLACK_WINS,
        STALEMATE,
        IDLE
    }
    public ChessBoard() {
        this(new VirtualAiController("Exaple"), new VirtualAiController("Exaple"));
    }

    public ChessBoard(ChessController whiteController, ChessController blackController) {
        this(board -> {
            for (int x = 0; x < 8; x++) {
                board.placePiece(new PawnChessPiece(false, board), x, 1);
                board.placePiece(new PawnChessPiece(true, board), x, 6);
            }

            board.placePiece(new RookChessPiece(false, board), 0, 0);
            board.placePiece(new KnightChessPiece(false, board), 1, 0);
            board.placePiece(new BishopChessPiece(false, board), 2, 0);
            board.placePiece(new KingChessPiece(false, board), 3, 0);
            board.placePiece(new QueenChessPiece(false, board), 4, 0);
            board.placePiece(new BishopChessPiece(false, board), 5, 0);
            board.placePiece(new KnightChessPiece(false, board), 6, 0);
            board.placePiece(new RookChessPiece(false, board), 7, 0);

            board.placePiece(new RookChessPiece(true, board), 0, 7);
            board.placePiece(new KnightChessPiece(true, board), 1, 7);
            board.placePiece(new BishopChessPiece(true, board), 2, 7);
            board.placePiece(new KingChessPiece(true, board), 3, 7);
            board.placePiece(new QueenChessPiece(true, board), 4, 7);
            board.placePiece(new BishopChessPiece(true, board), 5, 7);
            board.placePiece(new KnightChessPiece(true, board), 6, 7);
            board.placePiece(new RookChessPiece(true, board), 7, 7);
        }, blackController, whiteController);
    }

    public ChessBoard(@NotNull Consumer<ChessBoard> startingOder, ChessController whiteController, ChessController blackController) {
        this.startingOder = startingOder;
        this.whiteTurn = true;

        this.whiteController = whiteController;
        this.blackController = blackController;

    }

    public void tick() {
        if (state != State.PLAYING) return;

        if (whiteTurn) {
            whiteController.tick(this);
        } else {
            blackController.tick(this);
        }
    }


    public void sensorTick(boolean[] sensorFeedback) { // for latter impl with sensors
        if (state != State.PLAYING) return;

        if (whiteTurn && whiteController instanceof SensorController sensorController) {
            sensorController.sensorTick(sensorFeedback, this);
        } else if (blackController instanceof SensorController sensorController) {
            sensorController.sensorTick(sensorFeedback, this);
        }
    }

    public void start() {
        if (state == State.PLAYING) return;

        clear();

        this.whiteTurn = true;
        this.state = State.PLAYING;
        this.startingOder.accept(this);
    }

    public void clear() {

        moveHistory.clear();

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                board[row][col] = null;
            }
        }

    }

    public void placePiece(ChessPiece piece, int x, int y) {
        if (isValidCoord(x, y)) {
            board[y][x] = piece;
        }
    }

    public void movePiece(int fromX, int fromY, int toX, int toY) {
        movePiece(new ChessMove(fromX, fromY, toX, toY, this));
    }

    public void movePiece(@NotNull ChessMove move) {
        if (state != State.PLAYING) {
            System.out.println("The Game is not running!");
            return;
        }

        if (!isValidCoord(move.getFromX(), move.getFromY()) || !isValidCoord(move.getToX(), move.getToY())) {
            System.out.println("The coordinate is outside the board");
            return;
        }

        ChessPiece piece = board[move.getFromY()][move.getFromX()];
        if (piece == null) return;

        if (!piece.isValidMove(move.getToX(), move.getToY())) {
            System.out.println(piece.getName() + " can move to " + Arrays.deepToString(piece.getValidFields()) + " not to [" + move.getToX() + ", " + move.getToY() + "]");
            return;
        }

        if (!isMoveLegalRegardingCheck(move)) {
            System.out.println("Move ist illegal: King in chess!");
            return;
        }

        System.out.println(piece.getName() + " can move to " + Arrays.deepToString(piece.getValidFields()));


        board[move.getToY()][move.getToX()] = piece;
        board[move.getFromY()][move.getFromX()] = null;

        moveHistory.add(move);

        piece.hasMovedYet = true;

        whiteTurn = !whiteTurn;

        checkGameEnd();
    }

    public boolean isMoveLegalRegardingCheck(@NotNull ChessMove move) {

        ChessPiece movingPiece = board[move.getFromY()][move.getFromX()];
        ChessPiece capturedPiece = board[move.getToY()][move.getToX()];

        board[move.getToY()][move.getToX()] = movingPiece;
        board[move.getFromY()][move.getFromX()] = null;

        boolean isLegal = !isKingInCheck(movingPiece.isWhite());

        board[move.getFromY()][move.getFromX()] = movingPiece;
        board[move.getToY()][move.getToX()] = capturedPiece;

        return isLegal;
    }

    public boolean isKingInCheck(boolean whiteKing) {
        int kingX = -1, kingY = -1;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                ChessPiece piece = board[y][x];
                if (piece instanceof KingChessPiece && piece.isWhite() == whiteKing) {
                    kingX = x;
                    kingY = y;
                    break;
                }
            }
            if (kingX != -1) break;
        }

        if (kingX == -1) return false;

        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                ChessPiece piece = board[y][x];
                if (piece != null && piece.isWhite() != whiteKing) {
                    if (piece.isValidMove(kingX, kingY)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public ChessPiece getPiece(int x, int y) {
        if (!isValidCoord(x, y)) return null;
        return board[y][x];
    }

    public int getXForPiece(UUID uuid) {
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                ChessPiece piece = board[y][x];
                if (piece != null && piece.uuid.equals(uuid)) {
                    return x;
                }
            }
        }
        return -1; 
    }

    public int getYForPiece(UUID uuid) {
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                ChessPiece piece = board[y][x];
                if (piece != null && piece.uuid.equals(uuid)) {
                    return y;
                }
            }
        }
        return -1; 
    }

    public boolean isValidCoord(int x, int y) {
        return x >= 0 && x < 8 && y >= 0 && y < 8;
    }

    public List<ChessMove> getMoveHistory() {
        return new ArrayList<>(moveHistory);
    }

    public ChessPiece[][] getPieceBoard() {
        return board;
    }

    public int[][] getLevelBoard() {
        int[][] result = new int[8][8];
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                ChessPiece piece = board[y][x];
                result[y][x] = piece == null ? 0 : piece.getLevel() * (piece.isWhite() ? 1 : -1);
            }
        }
        return result;
    }

    public boolean[][] getNotNullBoard() {
        boolean[][] result = new boolean[8][8];
        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[row].length; col++) {
                result[row][col] = board[row][col] != null;
            }
        }
        return result;
    }

    public ChessPiece[] getPieceArray() {
        return Arrays.stream(board)
                .flatMap(Arrays::stream)
                .toArray(ChessPiece[]::new);
    }

    public int[] getLevelArray() {
        return Arrays.stream(board)
                .flatMapToInt(row -> Arrays.stream(row)
                        .mapToInt(p -> p == null ? 0 : p.getLevel() * (p.isWhite() ? 1 : -1)))
                .toArray();
    }

    public int[] getGameStateArray() {
            int[] result = new int[65];
            int[] boardValues = Arrays.stream(board)
                    .flatMapToInt(row -> Arrays.stream(Arrays.stream(row)
                            .mapToInt(p -> p == null ? 0 : p.getLevel() * (p.isWhite() ? 1 : -1))
                            .toArray())).toArray();
            System.arraycopy(boardValues, 0, result, 0, boardValues.length);
            result[64] = whiteTurn ? 1 : 0;
            return result;
    }

    public List<ChessPiece> getPieces(boolean white) {
        List<ChessPiece> pieces = new ArrayList<>();
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                ChessPiece piece = board[y][x];
                if (piece != null && piece.isWhite() == white) {
                    pieces.add(piece);
                }
            }
        }
        return pieces;
    }

    public String toVisualString() {
        StringBuilder sb = new StringBuilder();

        for (int y = 0; y < 8; y++) {
            char rowLabel = (char) ('1' + (7 - y));
            sb.append(rowLabel).append("  ");

            for (int x = 0; x < 8; x++) {
                ChessPiece piece = board[y][x];
                sb.append(piece != null ? piece.getColorChar() : '.').append("  ");
            }
            sb.append("\n");
        }

        sb.append("   ");
        for (int x = 0; x < 8; x++) {
            char colLabel = (char) ('a' + x);
            sb.append(colLabel).append("  ");
        }
        sb.append("\n");

        return sb.toString();
    }

    private void checkGameEnd() {
        boolean currentPlayerInCheck = isKingInCheck(whiteTurn);
        List<ChessMove> validMoves = getAllValidMoves(whiteTurn);

        if (validMoves.isEmpty()) {
            if (currentPlayerInCheck) {
                if (whiteTurn) {
                    state = State.CHECKMATE_BLACK_WINS;
                    lastGameWinner = blackController;
                    System.out.println("CHESSMATE! Black is winning!");
                } else {

                    state = State.CHECKMATE_WHITE_WINS;
                    lastGameWinner = whiteController;
                    System.out.println("CHESSMATE! White is winning!");
                }
            } else {
                state = State.STALEMATE;
                lastGameWinner = null;
                System.out.println("DRAW! - 0.5 Points for both!");
            }

        } else if (currentPlayerInCheck) {
            System.out.println((whiteTurn ? "White" : "Black") + " is in CHESS!");
        }
    }

    private @NotNull List<ChessMove> getAllValidMoves(boolean white) {
        List<ChessMove> validMoves = new ArrayList<>();

        for (int fromY = 0; fromY < 8; fromY++) {
            for (int fromX = 0; fromX < 8; fromX++) {
                ChessPiece piece = board[fromY][fromX];
                if (piece != null && piece.isWhite() == white) {
                    for (int toY = 0; toY < 8; toY++) {
                        for (int toX = 0; toX < 8; toX++) {
                            if (piece.isValidMove(toX, toY)) {
                                ChessMove testMove = new ChessMove(fromX, fromY, toX, toY, this);
                                if (isMoveLegalRegardingCheck(testMove)) {
                                    validMoves.add(testMove);
                                }
                            }
                        }
                    }
                }
            }
        }

        return validMoves;
    }

}
