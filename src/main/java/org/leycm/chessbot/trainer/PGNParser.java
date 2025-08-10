package org.leycm.chessbot.trainer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.leycm.chessbot.model.MoveEncoder;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;

public class PGNParser {

    public record Move(int from, int to, String notation) { }

    public record Game(List<Move> moves, GameResult result) { }

    public enum GameResult {
        WHITE_WIN,   // 1-0
        BLACK_WIN,   // 0-1
        DRAW,        // 1/2-1/2
        UNKNOWN      // * or no result
    }

    public enum MoveValue {
        WIN,
        LOSE,
        NEUTRAL
    }

    public record TrainingData(int[] boardState, int targetMove, MoveValue result) { }

    public static void parseFileInBatches(File pgnFile, int batchSize, Consumer<List<TrainingData>> batchProcessor) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(pgnFile), 64 * 1024)) {
            String line;
            StringBuilder gameText = new StringBuilder();
            List<TrainingData> currentBatch = new ArrayList<>();
            String gameResult = null;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("[Result")) {
                    int start = line.indexOf('"');
                    int end = line.lastIndexOf('"');
                    if (start != -1 && end != -1 && end > start) {
                        gameResult = line.substring(start + 1, end);
                    }
                } else if (line.startsWith("[")) {

                } else if (line.trim().isEmpty()) {
                    if (!gameText.isEmpty()) {
                        processGame(gameText.toString(), gameResult, currentBatch);
                        gameText = new StringBuilder();
                        gameResult = null;

                        if (currentBatch.size() >= batchSize) {
                            batchProcessor.accept(new ArrayList<>(currentBatch));
                            currentBatch.clear();
                        }
                    }
                } else {
                    gameText.append(line).append(" ");
                }
            }

            if (!gameText.isEmpty()) {
                processGame(gameText.toString(), gameResult, currentBatch);
            }

            if (!currentBatch.isEmpty()) {
                batchProcessor.accept(currentBatch);
            }
        }
    }

    private static void processGame(String gameText, String resultString, List<TrainingData> trainingData) {
        GameResult result = parseResult(resultString, gameText);
        Game game = parseGame(gameText, result);
        if (game == null) return;

        ChessBoard board = new ChessBoard();
        boolean whiteToMove = true;

        for (Move move : game.moves()) {
            int[] boardState = addTurnToBoard(board.toArray(), whiteToMove);
            int moveIndex = MoveEncoder.encodeMove(move.from(), move.to());

            MoveValue value = getMoveValue(game, whiteToMove);

            trainingData.add(new TrainingData(boardState, moveIndex, value));
            board.applyMove(move);
            whiteToMove = !whiteToMove;
        }
    }

    public static @NotNull MoveValue getMoveValue(Game game, boolean whiteToMove) {
        MoveValue value = MoveValue.LOSE;
        if (game.result() == GameResult.WHITE_WIN && whiteToMove) {
            value = MoveValue.WIN;
        } else if (game.result() == GameResult.BLACK_WIN && !whiteToMove) {
            value = MoveValue.WIN;
        } else if (game.result() == GameResult.DRAW && !whiteToMove) {
            value = MoveValue.NEUTRAL;
        } else if (game.result() == GameResult.UNKNOWN && !whiteToMove) {
            value = MoveValue.NEUTRAL;
        }
        return value;
    }

    private static int @NotNull [] addTurnToBoard(int[] boardState, boolean whiteToMove) {
        int[] extendedBoard = new int[65];
        System.arraycopy(boardState, 0, extendedBoard, 0, 64);
        extendedBoard[64] = whiteToMove ? 1 : 0;  // 1 for white, 0 for black
        return extendedBoard;
    }

    private static GameResult parseResult(String resultString, String gameText) {

        if (resultString != null) {
            return switch (resultString) {
                case "1-0" -> GameResult.WHITE_WIN;
                case "0-1" -> GameResult.BLACK_WIN;
                case "1/2-1/2" -> GameResult.DRAW;
                default -> GameResult.UNKNOWN;
            };
        }

        // Try to extract from game text
        if (gameText.contains("1-0")) {
            return GameResult.WHITE_WIN;
        } else if (gameText.contains("0-1")) {
            return GameResult.BLACK_WIN;
        } else if (gameText.contains("1/2-1/2")) {
            return GameResult.DRAW;
        }

        return GameResult.UNKNOWN;
    }

    // deprecated - use parseFileInBatches instead
    @Deprecated
    public static @NotNull List<Game> parseFile(File pgnFile) throws IOException {
        System.out.println("Warning: parseFile loads entire file into memory. Use parseFileInBatches for large files.");
        List<Game> games = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(pgnFile))) {
            String line;
            StringBuilder gameText = new StringBuilder();
            String gameResult = null;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("[Result")) {
                    int start = line.indexOf('"');
                    int end = line.lastIndexOf('"');
                    if (start != -1 && end != -1 && end > start) {
                        gameResult = line.substring(start + 1, end);
                    }
                } else if (line.startsWith("[")) {
                    continue;
                } else if (line.trim().isEmpty()) {
                    if (!gameText.isEmpty()) {
                        GameResult result = parseResult(gameResult, gameText.toString());
                        Game game = parseGame(gameText.toString(), result);
                        if (game != null) games.add(game);
                        gameText = new StringBuilder();
                        gameResult = null;
                    }
                } else {
                    gameText.append(line).append(" ");
                }
            }

            if (!gameText.isEmpty()) {
                GameResult result = parseResult(gameResult, gameText.toString());
                Game game = parseGame(gameText.toString(), result);
                if (game != null) games.add(game);
            }
        }

        return games;
    }

    public static @Nullable Game parseGame(@NotNull String gameText) {
        return parseGame(gameText, GameResult.UNKNOWN);
    }

    public static @Nullable Game parseGame(@NotNull String gameText, GameResult result) {
        List<Move> moves = new ArrayList<>();
        ChessBoard board = new ChessBoard();

        String cleanText = gameText.replaceAll("\\d+\\.", "")
                .replaceAll("1-0|0-1|1/2-1/2", "")
                .replaceAll("\\{[^}]*}", "")
                .trim();

        String[] moveStrings = cleanText.split("\\s+");

        for (String moveStr : moveStrings) {
            if (moveStr.isEmpty()) continue;

            Move move = parseMove(moveStr, board);
            if (move != null) {
                moves.add(move);
                board.applyMove(move);
            }
        }

        return moves.isEmpty() ? null : new Game(moves, result);
    }

    private static @Nullable Move parseMove(String notation, ChessBoard board) {
        notation = notation.trim();
        if (notation.isEmpty()) return null;

        notation = notation.replaceAll("[+#!?]", "");

        if (notation.equals("O-O")) {
            return board.isWhiteToMove() ? new Move(4, 6, "O-O") : new Move(60, 62, "O-O");
        }
        if (notation.equals("O-O-O")) {
            return board.isWhiteToMove() ? new Move(4, 2, "O-O-O") : new Move(60, 58, "O-O-O");
        }

        if (notation.length() == 2 && Character.isLowerCase(notation.charAt(0))) {
            int to = MoveEncoder.stringToSquare(notation);
            int from = findPawnFrom(to, board);
            return from != -1 ? new Move(from, to, notation) : null;
        }

        if (notation.length() == 4 && notation.charAt(1) == 'x') {
            int to = MoveEncoder.stringToSquare(notation.substring(2));
            int fromFile = notation.charAt(0) - 'a';
            int from = findPawnCaptureFrom(to, fromFile, board);
            return from != -1 ? new Move(from, to, notation) : null;
        }

        if (notation.length() >= 3) {
            char piece = notation.charAt(0);
            boolean isCapture = notation.contains("x");
            String targetSquare = notation.substring(notation.length() - 2);
            int to = MoveEncoder.stringToSquare(targetSquare);

            String disambig = "";
            int start = 1;
            int end = isCapture ? notation.indexOf('x') : notation.length() - 2;
            if (end > start) {
                disambig = notation.substring(start, end);
            }

            int from = findPieceFrom(piece, to, disambig, board);
            return from != -1 ? new Move(from, to, notation) : null;
        }

        return null;
    }

    private static int findPawnFrom(int to, @NotNull ChessBoard board) {
        boolean isWhite = board.isWhiteToMove();
        int[] boardArray = board.toArray();
        int pawnValue = isWhite ? 1 : 11;

        int oneBack = isWhite ? to - 8 : to + 8;
        if (oneBack >= 0 && oneBack < 64 && boardArray[oneBack] == pawnValue) {
            return oneBack;
        }

        int twoBack = isWhite ? to - 16 : to + 16;
        if (twoBack >= 0 && twoBack < 64 && boardArray[twoBack] == pawnValue) {
            int rank = to / 8;
            if ((isWhite && rank == 3) || (!isWhite && rank == 4)) {
                return twoBack;
            }
        }

        return -1;
    }

    private static int findPawnCaptureFrom(int to, int fromFile, ChessBoard board) {
        boolean isWhite = board.isWhiteToMove();
        int[] boardArray = board.toArray();
        int pawnValue = isWhite ? 1 : 11;

        int fromRank = isWhite ? (to / 8) - 1 : (to / 8) + 1;
        if (fromRank < 0 || fromRank > 7) return -1;

        int from = fromRank * 8 + fromFile;
        if (from >= 0 && from < 64 && boardArray[from] == pawnValue) {
            return from;
        }

        return -1;
    }

    private static int findPieceFrom(char piece, int to, String disambig, ChessBoard board) {
        boolean isWhite = board.isWhiteToMove();
        int pieceValue = getPieceValue(piece, isWhite);
        if (pieceValue == -1) return -1;

        int[] boardArray = board.toArray();
        List<Integer> candidates = new ArrayList<>();

        for (int from = 0; from < 64; from++) {
            if (boardArray[from] == pieceValue && canPieceMoveTo(piece, from, to, board)) {
                candidates.add(from);
            }
        }

        if (candidates.isEmpty()) return -1;
        if (candidates.size() == 1) return candidates.getFirst();

        if (!disambig.isEmpty()) {
            if (disambig.length() == 1) {
                if (Character.isDigit(disambig.charAt(0))) {

                    int rank = disambig.charAt(0) - '1';
                    for (int candidate : candidates) {
                        if (candidate / 8 == rank) return candidate;
                    }
                } else {
                    int file = disambig.charAt(0) - 'a';
                    for (int candidate : candidates) {
                        if (candidate % 8 == file) return candidate;
                    }
                }
            } else {
                int square = MoveEncoder.stringToSquare(disambig);
                if (candidates.contains(square)) return square;
            }
        }

        return candidates.getFirst();
    }

    private static int getPieceValue(char piece, boolean isWhite) {
        int offset = isWhite ? 0 : 10;
        return switch (piece) {
            case 'K' -> 6 + offset;
            case 'Q' -> 5 + offset;
            case 'R' -> 4 + offset;
            case 'B' -> 3 + offset;
            case 'N' -> 2 + offset;
            default -> -1;
        };
    }

    private static boolean canPieceMoveTo(char piece, int from, int to, ChessBoard board) {
        int fromFile = from % 8;
        int fromRank = from / 8;
        int toFile = to % 8;
        int toRank = to / 8;

        int dx = Math.abs(toFile - fromFile);
        int dy = Math.abs(toRank - fromRank);

        return switch (piece) {
            case 'K' -> dx <= 1 && dy <= 1;
            case 'Q' -> (dx == 0 || dy == 0 || dx == dy) && isPathClear(from, to, board);
            case 'R' -> (dx == 0 || dy == 0) && isPathClear(from, to, board);
            case 'B' -> dx == dy && isPathClear(from, to, board);
            case 'N' -> (dx == 2 && dy == 1) || (dx == 1 && dy == 2);
            default -> false;
        };
    }

    private static boolean isPathClear(int from, int to, ChessBoard board) {
        int[] boardArray = board.toArray();

        int fromFile = from % 8;
        int fromRank = from / 8;
        int toFile = to % 8;
        int toRank = to / 8;

        int dx = Integer.compare(toFile, fromFile);
        int dy = Integer.compare(toRank, fromRank);

        int current = from + dx + dy * 8;
        while (current != to) {
            if (boardArray[current] != 0) return false;
            current += dx + dy * 8;
        }

        return true;
    }
}