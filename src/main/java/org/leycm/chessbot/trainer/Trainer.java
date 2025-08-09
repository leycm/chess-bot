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

    // (-1 = keins)
    private static final int MAX_GAMES_LIMIT = 2000000;

    private static long totalSamplesProcessed = 0;
    private static long fileSize = 0;
    private static long bytesProcessed = 0;
    private static int gamesProcessed = 0;

    private static boolean stopTraining = false;

    public static void main(String[] args) throws IOException {
        System.out.println("=== Optimized Chess AI Trainer (Low Resource Usage) ===");

        checkMemorySettings();

        boolean useGPU = false;
        System.out.println("GPU Support: Disabled (CPU-only mode for lower resource usage)");

        int[] layerSizes = {64, 128, 64, 2048};
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
        System.out.println("Starting optimized streaming training...");

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
            int gamesInCurrentChunk = 0;
            long lastMemoryCheck = System.currentTimeMillis();

            while (!stopTraining && (line = reader.readLine()) != null) {
                bytesProcessed += line.getBytes().length + 1;

                if (line.startsWith("[")) {
                    continue;
                } else if (line.trim().isEmpty()) {
                    if (!gameBuffer.isEmpty()) {
                        processGameToBuffer(gameBuffer.toString(), miniBuffer);
                        gameBuffer.setLength(0);
                        gamesInCurrentChunk++;
                        gamesProcessed++;

                        // Check limit
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

    private static void processGameToBuffer(String gameText, List<PGNParser.TrainingData> buffer) {
        try {
            PGNParser.Game game = PGNParser.parseGame(gameText);
            if (game == null || game.moves().size() < 5) return;

            ChessBoard board = new ChessBoard();
            int moveCount = 0;

            for (PGNParser.Move move : game.moves()) {
                if (moveCount++ > 25) break;

                int[] boardState = board.toArray();
                int moveIndex = MoveEncoder.encodeMove(move.from(), move.to());

                buffer.add(new PGNParser.TrainingData(boardState, moveIndex));
                board.applyMove(move);
            }
        } catch (Exception _) {
        }
    }

    private static void processTrainingBatch(@NotNull List<PGNParser.TrainingData> batch, Model model) {
        if (batch.isEmpty()) return;

        Collections.shuffle(batch);

        int numBatches = (batch.size() + BATCH_SIZE - 1) / BATCH_SIZE;

        for (int b = 0; b < numBatches; b++) {
            int start = b * BATCH_SIZE;
            int end = Math.min(start + BATCH_SIZE, batch.size());
            int actualBatchSize = end - start;

            int[][] inputs = new int[actualBatchSize][];
            int[] targets = new int[actualBatchSize];

            for (int i = 0; i < actualBatchSize; i++) {
                PGNParser.TrainingData data = batch.get(start + i);
                inputs[i] = data.boardState();
                targets[i] = data.targetMove();
            }

            model.trainBatch(inputs, targets, actualBatchSize);
            totalSamplesProcessed += actualBatchSize;
        }
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

