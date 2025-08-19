package org.leycm.chessbot.board;

import org.leycm.chessbot.chess.ChessBoard;
import org.leycm.chessbot.chess.ChessMove;

import java.util.ArrayList;
import java.util.List;

public class LedControl {

    private static List<int[]> activeLeds = new ArrayList<>();
    private static boolean isActive = false;



    /// Settings

    private static int delay = 250;



    /// Code

    private static void turnOn(int[] led) {

        if (new ChessBoard().isValidCoord(led[0], led[1])) {
            System.out.println(led[0] + " " + led[1] + " is't a valid Led");
        }

        isActive = true;

        activeLeds.add(led);

    }

    private static void turnOff(int[] led) {

        activeLeds.remove(led);

        if (activeLeds.isEmpty()) {
            isActive = false;
        }

    }

    public static void forceStop() {

        activeLeds.forEach(led -> {
            turnOff(led);
        });
    }

    public static void showMove(ChessMove move) throws InterruptedException {

        forceStop();

        int startY = move.getFromX();
        int startX = move.getFromY();

        int relativeX = move.getFromX() - move.getToX();
        int relativeY = move.getFromY() - move.getToY();

        int directionX = relativeX / relativeX;
        int directionY = relativeY / relativeY;

        while (isActive) {

            int currentX = startX;
            int currentY = startY;

            turnOn(new int[]{currentX, currentY});

            while (currentX != startX + relativeX) {

                Thread.sleep(delay);

                forceStop();
                currentX = currentX + directionX;

                turnOn(new int[]{currentX, currentY});

            }

            while (currentY != startY + relativeY) {

                Thread.sleep(delay);


                forceStop();
                currentY = currentY + directionY;

                turnOn(new int[]{currentX, currentY});

            }

            Thread.sleep(delay * 5L);

        }

    }

    public static void showMate(int[] pos) throws InterruptedException {

        forceStop();

        while (isActive) {

            turnOn(pos);

            Thread.sleep(delay * 4L);

            turnOff(pos);

            Thread.sleep(delay * 4L);

        }
    }

    public static void showCheckmate(int[] pos) throws InterruptedException {

        forceStop();

        while (isActive) {

            turnOn(pos);

            Thread.sleep(delay * 2L);

            turnOff(pos);

            Thread.sleep(delay * 2L);

        }
    }

}
