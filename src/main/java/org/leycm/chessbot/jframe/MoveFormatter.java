package org.leycm.chessbot.jframe;

import org.jetbrains.annotations.NotNull;
import org.leycm.chessbot.chess.ChessMove;

/**
 * Utility class for formatting chess moves in different notations.
 */
public final class MoveFormatter {

    /**
     * Format a move in short notation (e.g., "e2-e4").
     */
    public static @NotNull String formatMoveShort(@NotNull ChessMove move) {
        char fromFile = (char) ('a' + move.getFromX());
        char toFile = (char) ('a' + move.getToX());
        int fromRank = 8 - move.getFromY();
        int toRank = 8 - move.getToY();

        return String.format("%c%d-%c%d", fromFile, fromRank, toFile, toRank);
    }

    /**
     * Format a move with move number (e.g., "1. e2-e4").
     */
    public static @NotNull String formatMoveWithNumber(@NotNull ChessMove move, int moveNumber) {
        char fromFile = (char) ('a' + move.getFromX());
        char toFile = (char) ('a' + move.getToX());
        int fromRank = 8 - move.getFromY();
        int toRank = 8 - move.getToY();

        return String.format("%d. %c%d-%c%d", moveNumber, fromFile, fromRank, toFile, toRank);
    }

    /**
     * Convert board coordinates to chess notation (e.g., x=0, y=7 -> "a1").
     */
    public static @NotNull String coordinatesToChessNotation(int x, int y) {
        char file = (char) ('a' + x);
        int rank = 8 - y;
        return String.format("%c%d", file, rank);
    }

    /**
     * Get the file letter from x coordinate (0 = 'a', 1 = 'b', etc.).
     */
    public static char getFile(int x) {
        return (char) ('a' + x);
    }

    /**
     * Get the rank number from y coordinate (y=0 -> rank 8, y=7 -> rank 1).
     */
    public static int getRank(int y) {
        return 8 - y;
    }

    // Private constructor to prevent instantiation
    private MoveFormatter() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
