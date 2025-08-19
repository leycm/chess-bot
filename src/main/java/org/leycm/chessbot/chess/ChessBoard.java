package org.leycm.chessbot.chess;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.leycm.chessbot.chess.controller.SensorController;
import org.leycm.chessbot.chess.controller.VirtualAiController;
import org.leycm.chessbot.chess.controller.VirtualUiController;
import org.leycm.chessbot.chess.pieces.*;

import javax.swing.Timer;
import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class ChessBoard implements Serializable {

    private final ChessPiece[][] board = new ChessPiece[8][8];
    private final List<ChessMove> moveHistory = new ArrayList<>();
    @Getter private final Consumer<ChessBoard> startingOder;

    @Getter @Setter private boolean whiteTurn;
    @Getter private ChessBoard.State state = State.START;


    @Getter @Setter private ChessController whiteController;
    @Getter @Setter private ChessController blackController;

    @Getter private ChessController lastGameWinner = null;

    public enum State implements Serializable {
        START,
        PLAYING,
        CHECKMATE_WHITE_WINS,
        CHECKMATE_BLACK_WINS,
        STALEMATE,
        IDLE
    }
    public ChessBoard() {
        this(new VirtualUiController("WhiteController"), new VirtualAiController("BlackController"));
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
        }, whiteController, blackController);
    }

    public ChessBoard(@NotNull Consumer<ChessBoard> startingOder, ChessController whiteController, ChessController blackController) {
        this.startingOder = startingOder;
        this.whiteTurn = true;

        this.whiteController = whiteController;
        this.blackController = blackController;
        start();
    }

    @Deprecated @ApiStatus.Internal
    public void autoTick() {
        new Timer(20, _ -> tick()).start();
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
        restart();
    }

    public void restart() {
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

    public boolean isAiTurn() {
        return getControllerForTurn() instanceof VirtualAiController ||
                getControllerForTurn() instanceof VirtualAiController; // for SensorAiController
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

    public ChessController getControllerForTurn() {
        if (whiteTurn) return whiteController;
        return blackController;
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
        return new int[][]{Arrays.stream(board)
                .mapToInt(row -> Arrays.stream(row)
                        .mapToInt(p -> p == null ? 0 : p.getLevel() * (p.isWhite() ? 1 : -1)).sum())
                .toArray()};
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

    public boolean[] getNotNullArray() {
        boolean[] result = new boolean[64];
        int[] level = getLevelArray();

        IntStream.range(0, result.length)
                .mapToObj(i -> result[i] = level[1] != 0);

        return result;
    }

    public int[] getGameStateArray() {
        int[] result = new int[65];
        int[] level = getLevelArray();

        System.arraycopy(level, 0, result, 0, level.length);
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

    private void checkGameEnd() {
        boolean currentPlayerInCheck = isKingInCheck(whiteTurn);
        List<ChessMove> validMoves = getAllValidMoves(whiteTurn);

        if (!validMoves.isEmpty()) return;

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
    }

    private @NotNull List<ChessMove> getAllValidMoves(boolean white) {
        List<ChessMove> validMoves = new ArrayList<>();
        for (ChessPiece piece : getPieces(white)) {
            validMoves.addAll(Arrays.asList(piece.getValidMoves()));
        }

        return validMoves;
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

}
