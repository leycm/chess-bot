package org.leycm.chessbot.trainer;

import org.jetbrains.annotations.NotNull;
import org.leycm.chessbot.model.Model;
import org.leycm.chessbot.model.ModelLoader;
import org.leycm.chessbot.model.MoveEncoder;

import java.io.*;
import java.time.LocalDate;
import java.util.*;

import java.util.zip.GZIPInputStream;

public class Trainer {

    private static final int BATCH_SIZE = 128;
    private static final int TRAINING_DATA_BUFFER_SIZE = 500;
    private static final int MAX_GAMES_PER_CHUNK = 100;
    private static final int CHECKPOINT_INTERVAL = 50000;

    private static final long MAX_MEMORY_USAGE_MB = 1024;
    private static final int MEMORY_CHECK_INTERVAL = 1000;
    private static final int FORCE_GC_INTERVAL = 5000;

    // winner/loser moves
    private static final float WINNER_LEARNING_MULTIPLIER = 1.8f;
    private static final float LOSER_LEARNING_MULTIPLIER = -0.8f;
    private static final float NEUTRAL_LEARNING_MULTIPLIER = 0.8f;

    // (-1 = keins)
    private static final int MAX_GAMES_LIMIT = 20000;

    private static long totalSamplesProcessed = 0;
    private static long fileSize = 0;
    private static long bytesProcessed = 0;
    private static int gamesProcessed = 0;

    private static boolean stopTraining = false;

    public static void main(String[] args) throws IOException {
        System.out.println("=== Enhanced Chess AI Trainer (10 Hidden Layers + Winner/Loser Learning) ===");

        checkMemorySettings();

        boolean useGPU = false;
        System.out.println("GPU Support: Disabled (CPU-only mode for lower resource usage)");

        int[] layerSizes = {
                65,
                512,
                384,
                256,
                192,
                128,
                96,
                64,
                48,
                32,
                24,
                4096
        };

        System.out.printf("Network architecture: %s%n", Arrays.toString(layerSizes));
        System.out.printf("Total layers: %d (Input: %d, Hidden: %d, Output: %d)%n",
                layerSizes.length, 1, layerSizes.length - 2, 1);

        Model model = new Model(layerSizes, 0.001f, useGPU);

        File pgnFile = findLargestPGNFile();
        if (pgnFile == null || !pgnFile.exists()) {
            System.out.println("No large PGN file found!");
            System.out.println("Please download from: https://database.lichess.org/");
            System.out.println("Recommended: lichess_db_standard_rated_2025-XX.pgn.bz2");
            return;
        }

        fileSize = pgnFile.length();
        System.out.printf("Training on file: %s (%.2f GB)%n", pgnFile.getName(), fileSize / 1024.0 / 1024.0 / 1024.0);

        File existingModel = new File("models/checkpoint.model");
        if (existingModel.exists()) {
            System.out.println("Loading existing checkpoint...");
            model = ModelLoader.load(existingModel);
        }

        trainOnLargeFile(model, pgnFile, useGPU);
    }

    private static void trainOnLargeFile(Model model, File pgnFile, boolean useGPU) throws IOException {
        System.out.println("Starting optimized streaming training with winner/loser learning...");

        int epochs = 1;

        for (int epoch = 0; epoch < epochs && !stopTraining; epoch++) {
            System.out.println("=== Streaming Epoch " + (epoch + 1) + " ===");
            bytesProcessed = 0;
            totalSamplesProcessed = 0;
            gamesProcessed = 0;

            processFileInChunks(pgnFile, model, epoch);

            if (stopTraining) {
                System.out.println("\nTraining stopped due to game limit.");
                break;
            }

            System.out.printf("Epoch %d completed. Total samples: %,d%n", epoch + 1, totalSamplesProcessed);

            File epochModel = new File("model/epoch/" + (epoch + 1) + "_chess_bot-" + totalSamplesProcessed + "~" + LocalDate.now() + ".model");
            ModelLoader.save(model, epochModel);
            System.out.println("Epoch model saved: " + epochModel.getName());

            System.gc();
            Thread.yield();
        }

        ModelLoader.save(model, new File("model/trained/chess_bot-latest.model"));
        ModelLoader.save(model, new File("model/trained/chess_bot-" + totalSamplesProcessed + "~" + LocalDate.now() + ".model"));
        System.out.println("Training completed!");
        System.out.printf("Total samples processed: %,d%n", totalSamplesProcessed);
    }

    private static void processFileInChunks(@NotNull File pgnFile, Model model, int epoch) throws IOException {
        InputStream inputStream;

        if (pgnFile.getName().endsWith(".gz") || pgnFile.getName().endsWith(".bz2")) {
            if (pgnFile.getName().endsWith(".gz")) {
                inputStream = new GZIPInputStream(new FileInputStream(pgnFile));
            } else {
                inputStream = new FileInputStream(pgnFile);
                System.out.println("Warning: BZ2 decompression not implemented, reading as raw file");
            }
        } else {
            inputStream = new FileInputStream(pgnFile);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream), 64 * 1024)) {
            String line;
            StringBuilder gameBuffer = new StringBuilder(512);
            List<PGNParser.TrainingData> miniBuffer = new ArrayList<>();
            String gameResult = null;
            int gamesInCurrentChunk = 0;
            long lastMemoryCheck = System.currentTimeMillis();

            while (!stopTraining && (line = reader.readLine()) != null) {
                bytesProcessed += line.getBytes().length + 1;

                if (line.startsWith("[Result")) {
                    int start = line.indexOf('"');
                    int end = line.lastIndexOf('"');
                    if (start != -1 && end != -1 && end > start) {
                        gameResult = line.substring(start + 1, end);
                    }
                } else if (line.startsWith("[")) {
                    continue;
                } else if (line.trim().isEmpty()) {
                    if (!gameBuffer.isEmpty() && !gameBuffer.toString().contains("Bullet game\"]")) {
                        processGameToBuffer(gameBuffer.toString(), gameResult, miniBuffer);
                        gameBuffer.setLength(0);
                        gameResult = null;
                        gamesInCurrentChunk++;
                        gamesProcessed++;

                        if (MAX_GAMES_LIMIT > 0 && gamesProcessed >= MAX_GAMES_LIMIT) {
                            processTrainingBatch(miniBuffer, model);
                            miniBuffer.clear();
                            saveCheckpoint(model);
                            stopTraining = true;
                            break;
                        }

                        if (gamesProcessed % MEMORY_CHECK_INTERVAL == 0 && isMemoryLow()) {
                            System.out.println("Low memory detected, processing current buffer...");
                            if (!miniBuffer.isEmpty()) {
                                processTrainingBatch(miniBuffer, model);
                                miniBuffer.clear();
                            }
                            System.gc();
                        }

                        if (gamesProcessed % FORCE_GC_INTERVAL == 0) {
                            System.gc();
                            Thread.yield();
                        }

                        if (miniBuffer.size() >= TRAINING_DATA_BUFFER_SIZE) {
                            processTrainingBatch(miniBuffer, model);
                            miniBuffer.clear();

                            printProgress(epoch);

                            if (totalSamplesProcessed % CHECKPOINT_INTERVAL == 0) {
                                saveCheckpoint(model);
                                System.gc();
                            }
                        }

                        if (gamesInCurrentChunk >= MAX_GAMES_PER_CHUNK) {
                            gamesInCurrentChunk = 0;
                            System.gc();
                            Thread.yield();
                        }
                    }
                } else {
                    if (!gameBuffer.isEmpty()) gameBuffer.append(' ');
                    gameBuffer.append(line);
                }

                if (System.currentTimeMillis() - lastMemoryCheck > 30000) {
                    lastMemoryCheck = System.currentTimeMillis();
                    if (isMemoryLow()) {
                        System.out.println("Low memory detected, processing current buffer...");
                        if (!miniBuffer.isEmpty()) {
                            processTrainingBatch(miniBuffer, model);
                            miniBuffer.clear();
                        }
                        System.gc();
                        Thread.sleep(200);
                    }
                }
            }

            if (!stopTraining && !miniBuffer.isEmpty()) {
                processTrainingBatch(miniBuffer, model);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void processGameToBuffer(@NotNull String gameText, String gameResult, List<PGNParser.TrainingData> buffer) {

        try {
            PGNParser.GameResult result = parseGameResult(gameResult, gameText);
            PGNParser.Game game = PGNParser.parseGame(gameText, result);
            if (game == null || game.moves().size() < 5) return;

            ChessBoard board = new ChessBoard();
            int moveCount = 0;
            boolean whiteToMove = true;

            for (PGNParser.Move move : game.moves()) {
                if (moveCount++ > 25) break;

                int[] boardState = addTurnToBoard(board.toArray(), whiteToMove);
                int moveIndex = MoveEncoder.encodeMove(move.from(), move.to());

                buffer.add(new PGNParser.TrainingData(boardState, moveIndex, PGNParser.getMoveValue(game, whiteToMove)));
                board.applyMove(move);
                whiteToMove = !whiteToMove;
            }
        } catch (Exception _) {
        }
    }

    private static PGNParser.GameResult parseGameResult(String resultString, String gameText) {
        if (resultString != null) {
            return switch (resultString) {
                case "1-0" -> PGNParser.GameResult.WHITE_WIN;
                case "0-1" -> PGNParser.GameResult.BLACK_WIN;
                case "1/2-1/2" -> PGNParser.GameResult.DRAW;
                default -> PGNParser.GameResult.UNKNOWN;
            };
        }

        if (gameText.contains("1-0")) {
            return PGNParser.GameResult.WHITE_WIN;
        } else if (gameText.contains("0-1")) {
            return PGNParser.GameResult.BLACK_WIN;
        } else if (gameText.contains("1/2-1/2")) {
            return PGNParser.GameResult.DRAW;
        }

        return PGNParser.GameResult.UNKNOWN;
    }

    private static int @NotNull [] addTurnToBoard(int[] boardState, boolean whiteToMove) {
        int[] extendedBoard = new int[65];
        System.arraycopy(boardState, 0, extendedBoard, 0, 64);
        extendedBoard[64] = whiteToMove ? 1 : 0;
        return extendedBoard;
    }

    private static void processTrainingBatch(@NotNull List<PGNParser.TrainingData> batch, Model model) {
        if (batch.isEmpty()) return;

        Collections.shuffle(batch);

        int numBatches = (batch.size() + BATCH_SIZE - 1) / BATCH_SIZE;
        int winnerMoves = 0, loserMoves = 0, neutralMoves = 0;

        for (int b = 0; b < numBatches; b++) {
            int start = b * BATCH_SIZE;
            int end = Math.min(start + BATCH_SIZE, batch.size());
            int actualBatchSize = end - start;

            int[][] inputs = new int[actualBatchSize][];
            int[] targets = new int[actualBatchSize];
            float[] winnerMultipliers = new float[actualBatchSize];

            for (int i = 0; i < actualBatchSize; i++) {
                PGNParser.TrainingData data = batch.get(start + i);
                inputs[i] = data.boardState();
                targets[i] = data.targetMove();

                if (data.result() == PGNParser.MoveValue.WIN) {
                    winnerMultipliers[i] = WINNER_LEARNING_MULTIPLIER;
                    winnerMoves++;
                } else if (data.result() == PGNParser.MoveValue.NEUTRAL) {
                    winnerMultipliers[i] = NEUTRAL_LEARNING_MULTIPLIER;
                    neutralMoves++;
                } else {
                    winnerMultipliers[i] = LOSER_LEARNING_MULTIPLIER;
                    loserMoves++;
                }
            }

            model.trainBatch(inputs, targets, actualBatchSize, winnerMultipliers);
            totalSamplesProcessed += actualBatchSize;
        }

        System.out.printf(" -> Learning stats: Winner moves: %d, Neutral moves: %d, Loser moves: %d\n",
                winnerMoves, neutralMoves, loserMoves);
    }

    private static void printProgress(int epoch) {
        double progressPercent = (double) bytesProcessed / fileSize * 100.0;
        long samplesPerSecond = totalSamplesProcessed / Math.max(1, (System.currentTimeMillis() - startTime) / 1000);

        System.out.printf("\r[Epoch %d] %.2f%% | %,d samples | %,d samples/sec | Memory: %dMB | Games: %,d",
                epoch + 1, progressPercent, totalSamplesProcessed, samplesPerSecond, getUsedMemoryMB(), gamesProcessed);

        if (totalSamplesProcessed % 25000 == 0) {
            System.out.println();
        }
    }

    private static final long startTime = System.currentTimeMillis();

    private static void saveCheckpoint(Model model) {
        try {
            File checkpoint = new File("model/checkpoint.model");
            ModelLoader.save(model, checkpoint);
            System.out.printf(" [CHECKPOINT: %,d samples] ", totalSamplesProcessed);
        } catch (IOException e) {
            System.out.println(" [CHECKPOINT FAILED] ");
        }
    }

    private static boolean isMemoryLow() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long usedMemoryMB = usedMemory / 1024 / 1024;

        return usedMemoryMB > MAX_MEMORY_USAGE_MB || usedMemory > (maxMemory * 0.8);
    }

    private static long getUsedMemoryMB() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
    }

    private static void checkMemorySettings() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long maxMemoryGB = maxMemory / 1024 / 1024 / 1024;

        System.out.printf("JVM Max Memory: %d GB%n", maxMemoryGB);

        if (maxMemoryGB < 2) {
            System.out.println("WARNING: Very low memory allocation detected!");
            System.out.println("For optimal performance, recommend starting with:");
            System.out.println("java -Xmx2g -Xms1g -XX:+UseG1GC -XX:G1HeapRegionSize=16m Trainer");
        }
    }

    private static File findLargestPGNFile() {
        File currentDir = new File("./assets");
        File largestFile = null;
        long largestSize = 0;

        if (!currentDir.exists()) {
            currentDir = new File(".");
        }

        File[] files = currentDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().toLowerCase().contains(".pgn")) {
                    if (file.length() > largestSize) {
                        largestSize = file.length();
                        largestFile = file;
                    }
                }
            }
        }

        return largestFile;
    }
}