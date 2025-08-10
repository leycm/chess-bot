package org.leycm.chessbot.util;

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
        return Arrays.stream(array).anyMatch( l -> l.equals(entry));
    }

}
