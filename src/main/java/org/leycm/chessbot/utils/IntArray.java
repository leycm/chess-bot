package org.leycm.chessbot.utils;

import java.util.Arrays;

public class IntArray {

    public static int[][] addIntArrayToIntArraysOfArray(int[][] original, int[] newRow) {
        int[][] result = new int[original.length + 1][];
        for (int i = 0; i < original.length; i++) {
            result[i] = original[i];
        }
        result[original.length] = newRow;
        return result;
    }

    public static boolean containsIntArray(int[][] array2D, int[] row) {
        for (int[] candidate : array2D) {
            if (Arrays.equals(candidate, row)) {
                return true;
            }
        }
        return false;
    }
}
