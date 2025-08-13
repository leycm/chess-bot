package org.leycm.chessbot.util;

import org.jetbrains.annotations.Contract;

import java.util.Arrays;

public class ArrayUtils {

    public static boolean containsInArray(long entry, long[] array) {
        return Arrays.stream(array).anyMatch( element -> element == entry);
    }

    public static boolean containsInArray(int entry, int[] array) {
        return Arrays.stream(array).anyMatch( element -> element == entry);
    }

    @SafeVarargs
    public static <L> boolean containsInArray(L entry, L... array) {
        return Arrays.asList(array).contains(entry);
    }

    @Contract(pure = true)
    public static boolean containsInArray(int[] entry, int[][] array) {

        for (int[] element : array) {
            if (element.length == entry.length) {
                boolean match = true;
                for (int i = 0; i < element.length; i++) {
                    if (element[i] != entry[i]) {
                        match = false;
                        break;
                    }
                }
                if (match) return true;
            }
        }
        return false;
    }


}
