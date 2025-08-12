package org.leycm.chessbot.trainer.parser;

import org.jetbrains.annotations.NotNull;
import org.leycm.chessbot.trainer.ChessPgnParser;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MultiThreadPgnParser implements ChessPgnParser {
    private final Pattern HEADER_PATTERN = Pattern.compile("\\[([^]]+)]");
    private final Pattern MOVE_PATTERN = Pattern.compile("(\\d+\\.+)?\\s*([NBRQK]?[a-h]?[1-8]?x?[a-h][1-8](?:=[NBRQ])?[+#]?)");

    private final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private final int QUEUE_SIZE = 5000; // Buffer size for game queue

    @Override
    public void processPgnFile(String filename, GameProcessor processor) throws IOException {
        BlockingQueue<RawGameData> gameQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        ExecutorService readerExecutor = Executors.newSingleThreadExecutor();

        try {
            Future<?> readerFuture = readerExecutor.submit(() -> {
                try {
                    readGamesFromFile(filename, gameQueue);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            List<Future<?>> workerFutures = new ArrayList<>();
            for (int i = 0; i < THREAD_POOL_SIZE; i++) {
                Future<?> workerFuture = executor.submit(() -> processGamesFromQueue(gameQueue, processor));
                workerFutures.add(workerFuture);
            }

            try {
                readerFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Error reading PGN file", e);
            }

            for (Future<?> future : workerFutures) {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Error processing games", e);
                }
            }

        } finally {
            readerExecutor.shutdown();
            executor.shutdown();

            try {
                if (!readerExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    readerExecutor.shutdownNow();
                }
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                readerExecutor.shutdownNow();
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void readGamesFromFile(String filename, BlockingQueue<RawGameData> gameQueue) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            Map<String, String> headers = new HashMap<>();
            StringBuilder movesBuilder = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) {
                    if (!headers.isEmpty() && !movesBuilder.isEmpty()) {
                        try {
                            gameQueue.put(new RawGameData(new HashMap<>(headers), movesBuilder.toString()));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                        headers.clear();
                        movesBuilder.setLength(0);
                    }
                    continue;
                }

                if (line.startsWith("[")) {
                    parseHeader(line, headers);
                } else {
                    movesBuilder.append(line).append(" ");
                }
            }

            if (!headers.isEmpty() && !movesBuilder.isEmpty()) {
                try {
                    gameQueue.put(new RawGameData(new HashMap<>(headers), movesBuilder.toString()));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

        } finally {
            for (int i = 0; i < THREAD_POOL_SIZE; i++) {
                try {
                    gameQueue.put(RawGameData.POISON_PILL);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void processGamesFromQueue(@NotNull BlockingQueue<RawGameData> gameQueue, GameProcessor processor) {
        try {
            while (true) {
                RawGameData rawGame = gameQueue.take();

                if (rawGame == RawGameData.POISON_PILL) {
                    break;
                }

                processGame(rawGame.headers(), rawGame.movesText(), processor);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void parseHeader(String line, Map<String, String> headers) {
        Matcher matcher = HEADER_PATTERN.matcher(line);
        if (matcher.find()) {
            String content = matcher.group(1);
            String[] parts = content.split("\\s+", 2);
            if (parts.length == 2) {
                String key = parts[0];
                String value = parts[1];

                if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                headers.put(key, value);
            }
        }
    }

    private void processGame(Map<String, String> headers, String movesText, GameProcessor processor) {
        try {
            String timeControl = headers.get("TimeControl");
            if (timeControl != null && (timeControl.startsWith("60+") || timeControl.contains("bullet"))) {
                processor.onFilteredGame("bullet");
                return;
            }

            int whiteElo = parseElo(headers.get("WhiteElo"));
            int blackElo = parseElo(headers.get("BlackElo"));
            int whiteRatingDiff = parseRatingDiff(headers.get("WhiteRatingDiff"));
            int blackRatingDiff = parseRatingDiff(headers.get("BlackRatingDiff"));
            String result = headers.get("Result");
            String link = headers.get("Site");

            if (whiteElo == 0 || blackElo == 0) {
                processor.onFilteredGame("invalid");
                return;
            }

            List<String> moves = extractMoves(movesText);
            if (moves.isEmpty()) {
                processor.onFilteredGame("invalid");
                return;
            }

            GameData gameData = new GameData(whiteElo, blackElo, whiteRatingDiff, blackRatingDiff, result, link, moves);
            processor.processGame(gameData);

        } catch (Exception e) {
            processor.onFilteredGame("invalid");
        }
    }

    private @NotNull List<String> extractMoves(@NotNull String movesText) {
        List<String> moves = new ArrayList<>();
        String cleanText = movesText.replaceAll("\\{[^}]*\\}", "")
                .replaceAll("\\([^)]*\\)", "")
                .replaceAll("[0-1]/[0-1]-[0-1]/[0-1]", "")
                .replaceAll("1-0|0-1|1/2-1/2", "");

        Matcher matcher = MOVE_PATTERN.matcher(cleanText);
        while (matcher.find()) {
            String move = matcher.group(2);
            if (move != null && !move.isEmpty()) {
                moves.add(move.trim());
            }
        }

        return moves;
    }

    private int parseElo(String eloStr) {
        try {
            return eloStr != null ? Integer.parseInt(eloStr) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int parseRatingDiff(String diffStr) {
        try {
            return diffStr != null ? Integer.parseInt(diffStr.replace("+", "")) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }


}