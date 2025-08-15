package org.leycm.chessbot.test.wolfram;


import org.leycm.chessbot.chess.ChessBoard;

import java.util.Arrays;
import java.util.stream.IntStream;

public class WolframTest {

    private static int[] convint2Dto1D(int[][] intArray2D) {
        int[] intArray1D = Arrays.stream(intArray2D)
                .flatMapToInt(Arrays::stream)
                .toArray();
        return intArray1D;
    }

    private static boolean[] convbool2Dto1D(boolean[][] boolArray2D) {
        int nRows = boolArray2D.length;
        int nCol = boolArray2D[0].length;
        int size = nRows * nCol;

        boolean[] boolArray1D = new boolean[size];

        for (int i = 0; i < boolArray1D.length; i++) {
            int row = Math.floorDiv(i, nRows);
            int col = Math.floorMod(i, nRows);
            boolArray1D[i] = boolArray2D[row][col];


        }
//        Boolean[] boolArray1D = Arrays.stream(boolArray2D)
//                .flatMap(row -> IntStream.range(0, row.length)
//                        .mapToObj(i -> row[i]))
//                .toArray(Boolean[]::new);

        return boolArray1D;
    }


    private static int[] sum(int[] a, int[] b) {
        int[] result = new int[a.length];
        Arrays.setAll(result, i -> a[i] + b[i]);
        return result;
    }

    private static int[] sub(int[] a, int[] b) {
        int[] result = new int[a.length];
        Arrays.setAll(result, i -> a[i] - b[i]);
        return result;
    }

    private static int[] prod(int[] a, int[] b) {
        int[] result = new int[a.length];
        Arrays.setAll(result, i   -> a[i] * b[i]);
        return result;
    }

    private static int[] booltoint(boolean[] boolArray) {
        int[] intArray = IntStream.range(0, boolArray.length)
                .mapToObj(i -> boolArray[i] ? 1 : 0)
                .mapToInt(Integer::intValue)
                .toArray();
        return intArray;
    }

    private static int[] mask(int[] inputArray, boolean[] booleanMask) {
         int[] intMask = booltoint(booleanMask);
         int[] result = prod(inputArray, intMask);
        return result;
    }

    private static int[] selfpawnmoves();
        boolean[][] sensordata1

    private static void arraytest(){
        int[][] myIntArray1 = {{11, 12}, {21, 22}};
        int[][] myIntArray2 = {{0, 1}, {5, 0}};
//        int[] my1DArray1 = Arrays.stream(myIntArray1)
//                .flatMapToInt(Arrays::stream)
//                .toArray();
//        int[] my1DArray2 = Arrays.stream(myIntArray2)
//                .flatMapToInt(Arrays::stream)
//                .toArray();
        boolean[][] myBoolArray2D = {{true, false}, {false, true}};
//        boolean[] myBoolArray1D = {true, false, false, true};
        int[] my1DIntMaskArray = {1, 0, 1, 1};

        int[][] resultArray = new int[2][2];
//        resultArray = new int;




        System.out.println("2D-Bool-Array:");
        System.out.println(Arrays.deepToString(myBoolArray2D));
        boolean[] myBoolArray1D = convbool2Dto1D(myBoolArray2D);
        System.out.println(Arrays.toString(myBoolArray1D));


            System.out.println("Ein Element:");
        System.out.println(myIntArray1[0][1]);


        System.out.println("myIntArray1 als 2D-Array::");
        System.out.println(Arrays.deepToString(myIntArray1));

        int[] my1DArray1 = convint2Dto1D(myIntArray1);
        System.out.println("myIntArray1 als 1D-Array:");
        System.out.println(Arrays.toString(my1DArray1));

        int[] my1DArray2 = convint2Dto1D(myIntArray2);
        System.out.println("myIntArray1 als 1D-Array:");
        System.out.println(Arrays.toString(my1DArray2));
//        System.out.println(Arrays.stream(myIntArray1).toList().toString());

        int[] result1D_sum = sum(my1DArray1, my1DArray2);
        System.out.println("sum:");
        System.out.println(Arrays.toString(sum(my1DArray1, my1DArray2)));
        System.out.println("sub:");
        System.out.println(Arrays.toString(sub(my1DArray1, my1DArray2)));
        System.out.println("prod:");
        System.out.println(Arrays.toString(prod(my1DArray1, my1DArray2)));
        System.out.println("mask with boolean array:");
        System.out.println(Arrays.toString(mask(my1DArray1, myBoolArray1D)));

    }


    public static void main(String[] args){

        arraytest();

    }
}

