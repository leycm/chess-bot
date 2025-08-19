package org.leycm.chessbot;

import org.jetbrains.annotations.NotNull;
import org.leycm.chessbot.chess.ChessBoard;
import org.leycm.chessbot.chess.ChessController;
import org.leycm.chessbot.chess.controller.VirtualUiController;
import org.leycm.chessbot.jframe.ChessBoardUi;

import java.time.LocalTime;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ChessBotApplication {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Consumer<Integer> tickLogic;
    private final Executor mainExecutor;

    private volatile boolean running = false;
    private final long tickIntervalNanos = TimeUnit.MILLISECONDS.toNanos(50);
    private long lastTickTime;


    public ChessBotApplication(Consumer<Integer> tickLogic, Executor mainExecutor) {
        this.tickLogic = tickLogic;
        this.mainExecutor = mainExecutor;
    }

    public void start() {
        if (running) return;
        running = true;
        lastTickTime = System.nanoTime();

        scheduler.execute(this::tickLoop);
    }

    public void stop() {
        running = false;
        scheduler.shutdownNow();
    }

    private void tickLoop() {
        while (running) {
            long now = System.nanoTime();
            long elapsed = now - lastTickTime;

            int ticksBehind = (int) (elapsed / tickIntervalNanos);

            if (ticksBehind > 0) {
                if (ticksBehind > 2) {
                    System.err.printf("\r[WARN]: Running %d ticks behind, trying to keep it up!", ticksBehind);
                }

                for (int i = 0; i < ticksBehind; i++) {
                    mainExecutor.execute(() -> tickLogic.accept(ticksBehind));
                }

                lastTickTime += (long) ticksBehind * tickIntervalNanos;
            }

            long sleepNanos = tickIntervalNanos - (System.nanoTime() - lastTickTime);
            if (sleepNanos > 0) {
                try {
                    TimeUnit.NANOSECONDS.sleep(sleepNanos);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }


    public static void runTickSim(long runtime) {
        Executor mainExecutor = Runnable::run;
        ChessBotApplication application = new ChessBotApplication(tick -> {

            int sleepTime = new Random().nextInt(90) + 1;
            System.out.printf("\r[TICK]: Running %d ticks behind, sleeping for %dms.", (tick - 1), sleepTime);

            try {Thread.sleep(sleepTime);
            } catch (InterruptedException _) {}

        }, mainExecutor);

        application.start();

        if (runtime > 0) {
            try {Thread.sleep(runtime);
            } catch (InterruptedException _) {}

            application.stop();
            System.out.printf("\n[INFO]: Stoped after %dsec \n", runtime / 1000 );
        }

    }

    public static void runChessUi(long runtime) {
        Executor mainExecutor = Runnable::run;
        ChessBoard board = new ChessBoard();

        ChessBotApplication application = new ChessBotApplication(_ -> board.tick(), mainExecutor);

        application.start();
        ChessBoardUi.streamBoard("main-ui", board);

        if (runtime > 0) {
            try {Thread.sleep(runtime);
            } catch (InterruptedException _) {}

            application.stop();
            System.out.printf("\n[INFO]: Stoped after %dsec \n", runtime / 1000 );
        }

    }

    public static void main(String @NotNull [] args) {
        String type = "virtual";
        long runtime = -1;

        for (String arg : args) {
            if (arg.startsWith("-type=")) type = arg.replace("-type=", "");
            if (arg.startsWith("-runtime=")) runtime = Long.parseLong(arg.replace("-runtime=", ""));
        }

        switch (type) {
            case "sim", "simulation", "tickSim" -> ChessBotApplication.runTickSim(runtime);
            case "ui", "gui", "virtual", "desc" -> ChessBotApplication.runChessUi(runtime);
            //case "board", "sensor", "realLife" -> ChessBotApplication.startTickSim(runtime);
            default -> throw new RuntimeException("The type \"" + type + "\" is not a valid run type");
        }
    }

}