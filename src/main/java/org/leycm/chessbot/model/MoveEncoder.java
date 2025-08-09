package org.leycm.chessbot.model;


import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class MoveEncoder {

    public static int encodeMove(int from, int to) {
        return from * 64 + to;
    }

    @Contract(value = "_ -> new", pure = true)
    public static int @NotNull [] decodeMove(int moveIndex) {
        int from = moveIndex / 64;
        int to = moveIndex % 64;
        return new int[]{from, to};
    }

    @Contract(pure = true)
    public static @NotNull String squareToString(int square) {
        int file = square % 8;
        int rank = square / 8;
        return (char)('a' + file) + "" + (rank + 1);
    }

    public static int stringToSquare(@NotNull String square) {
        int file = square.charAt(0) - 'a';
        int rank = square.charAt(1) - '1';
        return rank * 8 + file;
    }
}