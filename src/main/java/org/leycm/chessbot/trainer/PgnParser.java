package org.leycm.chessbot.trainer;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PgnParser {
    private static final Pattern HEADER_PATTERN = Pattern.compile("\\[([^\\]]+)\\]");
    private static final Pattern MOVE_PATTERN = Pattern.compile("(\\d+\\.+)?\\s*([NBRQK]?[a-h]?[1-8]?x?[a-h][1-8](?:=[NBRQ])?[+#]?)");

    public static void processPgnFile(String filename, GameProcessor processor) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            Map<String, String> headers = new HashMap<>();
            StringBuilder movesBuilder = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) {
                    if (!headers.isEmpty() && movesBuilder.length() > 0) {
                        processGame(headers, movesBuilder.toString(), processor);
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

            if (!headers.isEmpty() && movesBuilder.length() > 0) {
                processGame(headers, movesBuilder.toString(), processor);
            }
        }
    }

    private static void parseHeader(String line, Map<String, String> headers) {
        Matcher matcher = HEADER_PATTERN.matcher(line);
        if (matcher.find()) {
            String content = matcher.group(1);
            String[] parts = content.split("\\s+", 2);
            if (parts.length == 2) {
                String key = parts[0];
                String value = parts[1].replaceAll("\"", "");
                headers.put(key, value);
            }
        }
    }

    private static void processGame(Map<String, String> headers, String movesText, GameProcessor processor) {
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

            if (whiteElo == 0 || blackElo == 0) {
                processor.onFilteredGame("invalid");
                return;
            }

            List<String> moves = extractMoves(movesText);
            if (moves.isEmpty()) {
                processor.onFilteredGame("invalid");
                return;
            }

            GameData gameData = new GameData(whiteElo, blackElo, whiteRatingDiff, blackRatingDiff, result, moves);
            processor.processGame(gameData);

        } catch (Exception e) {
            processor.onFilteredGame("invalid");
        }
    }

    private static List<String> extractMoves(String movesText) {
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

    private static int parseElo(String eloStr) {
        try {
            return eloStr != null ? Integer.parseInt(eloStr) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int parseRatingDiff(String diffStr) {
        try {
            return diffStr != null ? Integer.parseInt(diffStr.replace("+", "")) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public interface GameProcessor {
        void processGame(GameData gameData);
        void onFilteredGame(String reason);
    }

    public static class GameData {
        public final int whiteElo;
        public final int blackElo;
        public final int whiteRatingDiff;
        public final int blackRatingDiff;
        public final String result;
        public final List<String> moves;

        public GameData(int whiteElo, int blackElo, int whiteRatingDiff, int blackRatingDiff, String result, List<String> moves) {
            this.whiteElo = whiteElo;
            this.blackElo = blackElo;
            this.whiteRatingDiff = whiteRatingDiff;
            this.blackRatingDiff = blackRatingDiff;
            this.result = result;
            this.moves = moves;
        }
    }
}