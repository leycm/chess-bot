package org.leycm.chessbot.trainer;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.leycm.chessbot.chess.*;
import org.leycm.chessbot.chess.pieces.*;
import org.leycm.chessbot.model.ChessModel;
import org.leycm.chessbot.model.ModelLoader;
import org.leycm.chessbot.model.MoveConverter;
import org.leycm.chessbot.trainer.parser.MultiThreadPgnParser;
import org.leycm.chessbot.trainer.parser.SingleThreadPgnParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Getter
public class ChessTrainer {
    private ChessModel model;

    private final AtomicLong gamesProcessed = new AtomicLong(0);
    private final AtomicLong samplesProcessed = new AtomicLong(0);
    private final AtomicInteger bulletFiltered = new AtomicInteger(0);
    private final AtomicInteger invalidFiltered = new AtomicInteger(0);
    private String lastGameLink;

    private final long startTime;
    private final String version;

    private final int autoSave = 10000;
    private long lastAutoSave = System.currentTimeMillis();


    public ChessTrainer(String version, String update) {
        this.version = version;
        this.startTime = System.currentTimeMillis();

        writeFirstLine("model/trained/models.info", version + ": " + update);

        try {
            this.model = ModelLoader.loadModel("model/trained/chess_model-" + version + ".model");
        } catch (IOException e) {
            this.model = new ChessModel();
        }
        startProgressReporting();
    }

    public void trainFromPgn(String pgnFilename, boolean multithreading) {

        ChessPgnParser parser = switch (String.valueOf(multithreading)) {
            case "true" -> new MultiThreadPgnParser();
            case "false" -> new SingleThreadPgnParser();
            default -> throw new IllegalStateException("Unexpected value: " + multithreading);
        };

        try {

            parser.processPgnFile(pgnFilename, new ChessPgnParser.GameProcessor() {
                @Override
                public void processGame(ChessPgnParser.GameData gameData) {
                    trainOnGame(gameData);
                    lastGameLink = gameData.link();
                    gamesProcessed.incrementAndGet();

                    if (gamesProcessed.get() % autoSave != 0) return;

                    try {
                        lastAutoSave = System.currentTimeMillis();
                        ModelLoader.saveModel(model, "model/trained/chess_model-" + version + ".model");
                    } catch (Exception _) {}


                }

                @Override
                public void onFilteredGame(String reason) {
                    if ("bullet".equals(reason)) {
                        bulletFiltered.incrementAndGet();
                    } else {
                        invalidFiltered.incrementAndGet();
                    }
                }
            });

            ModelLoader.saveModel(model, "model/trained/chess_model-" + version + ".model");

        } catch (IOException e) {
            System.err.println("Error processing PGN file: " + e.getMessage());
        }
    }

    private void trainOnGame(@NotNull SingleThreadPgnParser.GameData gameData) {
        ChessBoard board = new ChessBoard();
        setupInitialPosition(board);

        boolean whiteToMove = true;

        for (String moveStr : gameData.moves()) {
            int[] boardState = board.getBoardForAi();
            int[] move = MoveConverter.moveStringToArray(moveStr, board);

            if (move[0] == -1) continue;

            double reward = calculateReward(gameData, whiteToMove);

            if (board.getPiece(move[0], move[1]) != null) {
                board.movePiece(move[0], move[1], move[2], move[3]);
                model.train(boardState, move, reward);
                samplesProcessed.incrementAndGet();
            }

            whiteToMove = !whiteToMove;
        }
    }

    private double calculateReward(@NotNull SingleThreadPgnParser.GameData gameData, boolean whiteToMove) {
        double baseReward = 0.5;

        if ("1-0".equals(gameData.result())) {
            baseReward = whiteToMove ? 1.0 : 0.0;
        } else if ("0-1".equals(gameData.result())) {
            baseReward = whiteToMove ? 0.0 : 1.0;
        }

        int playerElo = whiteToMove ? gameData.whiteElo() : gameData.blackElo();
        int playerRatingDiff = whiteToMove ? gameData.whiteRatingDiff() : gameData.blackRatingDiff();

        double eloFactor = Math.max(0.1, Math.min(2.0, playerElo / 1500.0));
        double ratingFactor = 1.0 + (playerRatingDiff / 100.0);

        return baseReward * eloFactor * ratingFactor;
    }

    private void setupInitialPosition(ChessBoard board) {
        for (int x = 0; x < 8; x++) {
            board.placePiece(new PawnChessPiece(true, board), x, 6);
            board.placePiece(new PawnChessPiece(false, board), x, 1);
        }

        board.placePiece(new RookChessPiece(true, board), 0, 7);
        board.placePiece(new RookChessPiece(true, board), 7, 7);
        board.placePiece(new KnightChessPiece(true, board), 1, 7);
        board.placePiece(new KnightChessPiece(true, board), 6, 7);
        board.placePiece(new BishopChessPiece(true, board), 2, 7);
        board.placePiece(new BishopChessPiece(true, board), 5, 7);
        board.placePiece(new QueenChessPiece(true, board), 3, 7);
        board.placePiece(new KingChessPiece(true, board), 4, 7);

        board.placePiece(new RookChessPiece(false, board), 0, 0);
        board.placePiece(new RookChessPiece(false, board), 7, 0);
        board.placePiece(new KnightChessPiece(false, board), 1, 0);
        board.placePiece(new KnightChessPiece(false, board), 6, 0);
        board.placePiece(new BishopChessPiece(false, board), 2, 0);
        board.placePiece(new BishopChessPiece(false, board), 5, 0);
        board.placePiece(new QueenChessPiece(false, board), 3, 0);
        board.placePiece(new KingChessPiece(false, board), 4, 0);
    }

    private void startProgressReporting() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            long elapsed = System.currentTimeMillis() - startTime;
            long hours = elapsed / 3600000;
            long minutes = (elapsed % 3600000) / 60000;
            long seconds = (elapsed % 60000) / 1000;

            long saveSince = (System.currentTimeMillis() - lastAutoSave)  / 1000;
            String lastSaveSince = saveSince > 14 ? "" : "[Auto saved Model " + saveSince + "s]";

            long games = gamesProcessed.get();
            long samples = samplesProcessed.get();
            double speed = games > 0 ? (double) samples / (elapsed / 1000.0) : 0;
            long memory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);

            System.out.printf("\r[%02d:%02d:%02d][%dMB]%s Trained %d Samples in %d Games | Latest: %s | Speed: %.1f s/s | Filtered: %d bullet, %d invalid",
                    hours, minutes, seconds, memory, lastSaveSince, samples, games, lastGameLink, speed, bulletFiltered.get(), invalidFiltered.get());

        }, 1, 1, TimeUnit.SECONDS);
    }


    public static void writeFirstLine(String pathStr, String line) {
        try {
            Path path = Paths.get(pathStr);
            List<String> lines = null;
            lines = Files.exists(path) ? Files.readAllLines(path) : new ArrayList<>();
            lines.addFirst(line);
            Files.write(path, lines);
        } catch (Exception _) {}
    }

    public static void main(String @NotNull [] args) {

        if (args.length < 1) {
            System.out.println("Usage: java ChessTrainer <pgn_file> (--multithreading)");
        }

        String filename = args.length < 1 ? "assets/train_games.pgn" : args[0];
        boolean multithreading = Arrays.stream(args).anyMatch(arg -> arg.equalsIgnoreCase("--multithreading"));

        ChessTrainer trainer = new ChessTrainer("1.1.2-R0-SNAPSHOT", "Add link to Site in the stats");
        // versions in models.info

        trainer.trainFromPgn(filename, multithreading);
    }
}
