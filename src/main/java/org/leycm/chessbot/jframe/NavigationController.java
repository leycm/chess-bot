package org.leycm.chessbot.jframe;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.leycm.chessbot.chess.ChessBoard;
import org.leycm.chessbot.chess.ChessMove;

import java.util.List;

/**
 * Handles navigation through the chess game history and manages the display state.
 */
@Setter
@Getter
public class NavigationController {
    private ChessBoard chessBoard;
    private ChessBoard displayBoard;
    private int currentMoveIndex = -1; // -1 means at the latest position
    private final String id;

    public NavigationController(String id, ChessBoard chessBoard) {
        this.chessBoard = chessBoard;
        this.displayBoard = chessBoard;
        this.id = id;
    }

    public boolean isAtLatestPosition() {
        return currentMoveIndex == -1;
    }

    public boolean isAtStartPosition() {
        return currentMoveIndex == -2;
    }

    /**
     * Navigate to a specific move in the game history.
     * @param moveIndex The move index to navigate to (-1 = latest, -2 = start, >= 0 = specific move)
     */
    public void navigateToMove(int moveIndex) {
        List<ChessMove> history = chessBoard.getMoveHistory();

        if (moveIndex == -1) {
            currentMoveIndex = -1;
            displayBoard = chessBoard;
        } else if (moveIndex == -2) {
            currentMoveIndex = -2;
            displayBoard = createBoardAtMove(-2);
        } else if (moveIndex >= 0 && moveIndex < history.size()) {
            currentMoveIndex = moveIndex;
            displayBoard = createBoardAtMove(moveIndex);
        }
    }

    /**
     * Navigate to the first move.
     */
    public void navigateToFirst() {
        navigateToMove(-2);
    }

    /**
     * Navigate to the previous move.
     */
    public void navigateToPrevious() {
        List<ChessMove> history = chessBoard.getMoveHistory();
        if (currentMoveIndex == -1) navigateToMove(history.size() -1);
        navigateToMove(Math.max(-2, currentMoveIndex - 1));
    }

    /**
     * Navigate to the next move.
     */
    public void navigateToNext() {
        List<ChessMove> history = chessBoard.getMoveHistory();
        navigateToMove(history.size() > currentMoveIndex + 1 ? currentMoveIndex + 1 : -1 );
    }

    /**
     * Navigate to the last move.
     */
    public void navigateToLast() {
        navigateToMove(-1);
    }

    /**
     * Get the last move that was played.
     */
    public ChessMove getLastMove() {
        List<ChessMove> history = chessBoard.getMoveHistory();

        if (!history.isEmpty() && currentMoveIndex >= 0) {
            return history.get(currentMoveIndex);
        } else if (currentMoveIndex == -1 && !history.isEmpty()) {
            return history.getLast();
        }

        return null;
    }

    /**
     * Get the current turn status for display purposes.
     */
    public String getTurnStatus() {
        List<ChessMove> history = chessBoard.getMoveHistory();

        if (currentMoveIndex == -1) {
            return "Turn: " + (chessBoard.isWhiteTurn() ? "White" : "Black");
        } else if (currentMoveIndex == -2) {
            return "Turn: White (Start Position)";
        } else if (currentMoveIndex >= 0 && currentMoveIndex < history.size()) {
            boolean isWhiteTurn = (currentMoveIndex % 2) == 1; // After white's move, it's black's turn
            return "Turn: " + (isWhiteTurn ? "Black" : "White") + " (Move " + (currentMoveIndex + 1) + ")";
        }

        return "Turn: Unknown";
    }

    /**
     * Get the current game status for display purposes.
     */
    public String getGameStatus() {
        if (currentMoveIndex == -2) {
            return "Status: Start Position";
        } else if (currentMoveIndex >= 0) {
            return "Status: Viewing History";
        }

        return switch (chessBoard.getState()) {
            case START -> "Game Ready to Start";
            case PLAYING -> "Game in Progress";
            case CHECKMATE_WHITE_WINS -> "Checkmate! White Wins";
            case CHECKMATE_BLACK_WINS -> "Checkmate! Black Wins";
            case STALEMATE -> "Stalemate! Draw";
            case IDLE -> "Game Idle";
        };
    }

    /**
     * Reset the navigation to the latest position and clear the board state.
     */
    public void reset() {
        currentMoveIndex = -1;
        displayBoard = chessBoard;
    }

    private @NotNull ChessBoard createBoardAtMove(int moveIndex) {
        ChessBoard tempBoard = new ChessBoard(chessBoard.getStartingOder(),
                chessBoard.getWhiteController(),
                chessBoard.getBlackController());
        tempBoard.start();

        if (moveIndex == -2) {
            return tempBoard;
        }

        List<ChessMove> history = chessBoard.getMoveHistory();

        int maxMoves = (moveIndex == -1) ? history.size() - 1 : moveIndex;

        for (int i = 0; i <= maxMoves && i < history.size(); i++) {
            ChessMove move = history.get(i);
            tempBoard.movePiece(move.getFromX(), move.getFromY(), move.getToX(), move.getToY());
        }

        return tempBoard;
    }
}