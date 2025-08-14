package org.leycm.chessbot.trainer.parser;

import org.jetbrains.annotations.NotNull;
import org.leycm.chessbot.trainer.ChessPgnParser;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SingleThreadPgnParser implements ChessPgnParser {
    private final Pattern HEADER_PATTERN = Pattern.compile("\\[([^]]+)]");
    private final Pattern MOVE_PATTERN = Pattern.compile("(\\d+\\.+)?\\s*([NBRQK]?[a-h]?[1-8]?x?[a-h][1-8](?:=[NBRQ])?[+#]?)");

    public void processPgnFile(String filename, GameProcessor processor) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            Map<String, String> headers = new HashMap<>();
            StringBuilder movesBuilder = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) {
                    if (!headers.isEmpty() && !movesBuilder.isEmpty()) {
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

            if (!headers.isEmpty() && !movesBuilder.isEmpty()) {
                processGame(headers, movesBuilder.toString(), processor);
            }
        }
    }

    private void parseHeader(String line, Map<String, String> headers) {
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

    public static @NotNull List<String> extractMoves(@NotNull String pgn) {
        String cleaned = pgn.replaceAll("\\{[^}]*}", "");
        cleaned = cleaned.replaceAll("\\d+\\.\\.\\.|\\d+\\.", "");
        cleaned = cleaned.replaceAll("(1-0|0-1|1/2-1/2)", "");

        String[] tokens = cleaned.trim().split("\\s+");

        List<String> moves = new ArrayList<>();
        for (String token : tokens) {
            if (!token.isEmpty()) {
                moves.add(token);
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