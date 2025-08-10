package org.leycm.chessbot.test.model;

import org.leycm.chessbot.model.Model;
import org.leycm.chessbot.model.ModelLoader;
import org.leycm.chessbot.model.MoveEncoder;
import org.leycm.chessbot.trainer.ChessBoard;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ChessAITournament {

    private static final int MAX_MOVES_PER_GAME = 10000;
    private static final int TOP_MOVES_TO_CONSIDER = 3;
    private static final boolean SHOW_BOARD = true;
    private static final boolean SHOW_THINKING = true;

    public static void main(String[] args) {
        System.out.println("=== Chess AI Tournament ===");

        Model whiteAI = loadModel("model/trained/chess_bot-12089~2025-08-10.model", "White AI");
        Model blackAI = loadModel("model/trained/chess_bot-194911~2025-08-10.model", "Black AI");

        if (whiteAI == null || blackAI == null) {
            System.out.println("Could not load both models. Looking for any available models...");
            whiteAI = loadAnyAvailableModel("White AI");
            blackAI = loadAnyAvailableModel("Black AI");

            if (whiteAI == null || blackAI == null) {
                System.out.println("No trained models found! Please train models first.");
                return;
            }
        }

        playTournament(whiteAI, blackAI, 100);
    }

    private static Model loadModel(String path, String name) {
        try {
            File modelFile = new File(path);
            if (modelFile.exists()) {
                System.out.println("Loading " + name + " from: " + path);
                return ModelLoader.load(modelFile);
            }
        } catch (IOException e) {
            System.out.println("Failed to load " + name + ": " + e.getMessage());
        }
        return null;
    }

    private static Model loadAnyAvailableModel(String name) {
        // Try different common locations
        String[] possiblePaths = {
                "model/trained/chess_bot-latest.model",
                "model/checkpoint.model",
                "models/checkpoint.model",
                "chess_bot.model"
        };

        for (String path : possiblePaths) {
            Model model = loadModel(path, name);
            if (model != null) return model;
        }
        return null;
    }

    private static void playTournament(Model whiteAI, Model blackAI, int numberOfGames) {
        int whiteWins = 0, blackWins = 0, draws = 0;

        System.out.println("\n=== Starting Tournament: " + numberOfGames + " games ===");

        for (int gameNum = 1; gameNum <= numberOfGames; gameNum++) {
            System.out.println("\n" + "=".repeat(50));
            System.out.println("GAME " + gameNum + " / " + numberOfGames);
            System.out.println("=".repeat(50));

            GameResult result = playGame(whiteAI, blackAI, gameNum);

            switch (result) {
                case WHITE_WIN -> {
                    whiteWins++;
                    System.out.println("White AI wins!");
                }
                case BLACK_WIN -> {
                    blackWins++;
                    System.out.println("Black AI wins!");
                }
                case DRAW -> {
                    draws++;
                    System.out.println("Draw!");
                }
            }

            System.out.println("Current Score - White: " + whiteWins + ", Black: " + blackWins + ", Draws: " + draws);
        }

        System.out.println("\n" + "=".repeat(50));
        System.out.println("TOURNAMENT RESULTS");
        System.out.println("=".repeat(50));
        System.out.printf("White AI: %d wins (%.1f%%)%n", whiteWins, (whiteWins * 100.0 / numberOfGames));
        System.out.printf("Black AI: %d wins (%.1f%%)%n", blackWins, (blackWins * 100.0 / numberOfGames));
        System.out.printf("Draws: %d (%.1f%%)%n", draws, (draws * 100.0 / numberOfGames));
    }

    private static GameResult playGame(Model whiteAI, Model blackAI, int gameNumber) {
        ChessBoard board = new ChessBoard();
        List<String> moveHistory = new ArrayList<>();
        Map<String, Integer> positionHistory = new HashMap<>();

        if (SHOW_BOARD) {
            System.out.println("Starting position:");
            System.out.println(board);
        }

        for (int moveCount = 0; moveCount < MAX_MOVES_PER_GAME; moveCount++) {
            Model currentAI = board.isWhiteToMove() ? whiteAI : blackAI;
            String playerName = board.isWhiteToMove() ? "White" : "Black";

            String boardHash = Arrays.toString(board.toArrayWithTurn());
            positionHistory.put(boardHash, positionHistory.getOrDefault(boardHash, 0) + 1);
            if (positionHistory.get(boardHash) >= 3) {
                System.out.println("Draw by threefold repetition!");
                return GameResult.DRAW;
            }

            AIMove aiMove = getAIMove(currentAI, board, playerName);
            if (aiMove == null) {
                System.out.println(playerName + " has no legal moves!");
                if (isInCheck(board)) {
                    System.out.println("Checkmate! " + (board.isWhiteToMove() ? "Black" : "White") + " wins!");
                    return board.isWhiteToMove() ? GameResult.BLACK_WIN : GameResult.WHITE_WIN;
                } else {
                    System.out.println("Stalemate! Draw!");
                    return GameResult.DRAW;
                }
            }

            int[] move = MoveEncoder.decodeMove(aiMove.moveIndex);
            String moveNotation = MoveEncoder.squareToString(move[0]) + "->" + MoveEncoder.squareToString(move[1]);

            if (SHOW_THINKING) {
                System.out.printf("%s plays: %s (confidence: %.3f, considered %d moves)%n",
                        playerName, moveNotation, aiMove.confidence, aiMove.alternativesConsidered);
            }

            applyMove(board, move[0], move[1]);
            moveHistory.add(playerName + ": " + moveNotation);

            if (SHOW_BOARD && (moveCount < 10 || moveCount % 10 == 9)) {
                System.out.println("\nAfter move " + (moveCount + 1) + ":");
                System.out.println(board);
            }

            if (isGameOver(board)) {
                if (isInCheck(board)) {
                    System.out.println("Checkmate! " + (board.isWhiteToMove() ? "Black" : "White") + " wins!");
                    return board.isWhiteToMove() ? GameResult.BLACK_WIN : GameResult.WHITE_WIN;
                } else {
                    System.out.println("Stalemate! Draw!");
                    return GameResult.DRAW;
                }
            }
        }

        System.out.println("Game ended after " + MAX_MOVES_PER_GAME + " moves - Draw by move limit!");
        return GameResult.DRAW;
    }

    private static AIMove getAIMove(Model ai, ChessBoard board, String playerName) {
        int[] boardWithTurn = board.toArrayWithTurn();
        float[] predictions = ai.predict(boardWithTurn);

        List<MoveCandidate> candidates = new ArrayList<>();
        List<Integer> legalMoves = generateLegalMoves(board);

        if (legalMoves.isEmpty()) {
            return null;
        }

        for (int moveIndex : legalMoves) {
            if (moveIndex < predictions.length) {
                candidates.add(new MoveCandidate(moveIndex, predictions[moveIndex]));
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        candidates.sort((a, b) -> Float.compare(b.probability, a.probability));

        int topN = Math.min(TOP_MOVES_TO_CONSIDER, candidates.size());
        List<MoveCandidate> topMoves = candidates.subList(0, topN);

        if (SHOW_THINKING) {
            System.out.printf("%s thinking: ", playerName);
            for (int i = 0; i < Math.min(3, topMoves.size()); i++) {
                MoveCandidate move = topMoves.get(i);
                int[] decoded = MoveEncoder.decodeMove(move.moveIndex);
                System.out.printf("%s(%.3f) ",
                        MoveEncoder.squareToString(decoded[0]) + "->" + MoveEncoder.squareToString(decoded[1]),
                        move.probability);
            }
            System.out.println();
        }

        MoveCandidate selectedMove = weightedRandomSelection(topMoves);
        return new AIMove(selectedMove.moveIndex, selectedMove.probability, topN);
    }

    private static MoveCandidate weightedRandomSelection(List<MoveCandidate> moves) {
        float totalWeight = 0;
        for (MoveCandidate move : moves) {
            totalWeight += Math.max(0.1f, move.probability);
        }

        float random = (float) Math.random() * totalWeight;
        float currentWeight = 0;

        for (MoveCandidate move : moves) {
            currentWeight += Math.max(0.1f, move.probability);
            if (random <= currentWeight) {
                return move;
            }
        }

        return moves.getFirst();
    }

    private static List<Integer> generateLegalMoves(ChessBoard board) {
        List<Integer> legalMoves = new ArrayList<>();

        for (int from = 0; from < 64; from++) {
            if (!board.isOccupied(from)) continue;

            boolean isWhitePiece = board.isWhitePiece(from);
            if (isWhitePiece != board.isWhiteToMove()) continue;

            for (int to = 0; to < 64; to++) {
                if (from == to) continue;

                if (isBasicLegalMove(board, from, to)) {
                    if (board.isOccupied(to)) {
                        if (board.isWhitePiece(to) == board.isWhiteToMove()) {
                            continue;
                        }
                    }

                    ChessBoard testBoard = copyBoard(board);
                    applyMove(testBoard, from, to);
                    if (!isInCheck(testBoard, !testBoard.isWhiteToMove())) {
                        legalMoves.add(MoveEncoder.encodeMove(from, to));
                    }
                }
            }
        }

        return legalMoves;
    }

    private static boolean isBasicLegalMove(ChessBoard board, int from, int to) {
        int piece = board.getPieceAt(from);
        int fromRank = from / 8, fromFile = from % 8;
        int toRank = to / 8, toFile = to % 8;
        int rankDiff = toRank - fromRank;
        int fileDiff = toFile - fromFile;
        int absRankDiff = Math.abs(rankDiff);
        int absFileDiff = Math.abs(fileDiff);

        return switch (piece % 10) {
            case 1 -> {
                boolean isWhite = piece <= 6;
                int direction = isWhite ? 1 : -1;
                if (fileDiff == 0) {
                    if (rankDiff == direction && !board.isOccupied(to)) yield true;
                    if (rankDiff == 2 * direction && !board.isOccupied(to) &&
                            ((isWhite && fromRank == 1) || (!isWhite && fromRank == 6))) yield true;
                } else if (absFileDiff == 1 && rankDiff == direction && board.isOccupied(to)) {
                    yield true;
                }
                yield false;
            }
            case 2 -> absRankDiff * absFileDiff == 2;
            case 3 -> absRankDiff == absFileDiff && isClearPath(board, from, to);
            case 4 -> (rankDiff == 0 || fileDiff == 0) && isClearPath(board, from, to);
            case 5 -> (absRankDiff == absFileDiff || rankDiff == 0 || fileDiff == 0) && isClearPath(board, from, to);
            case 6 -> absRankDiff <= 1 && absFileDiff <= 1;
            default -> false;
        };
    }

    private static boolean isClearPath(ChessBoard board, int from, int to) {
        int fromRank = from / 8, fromFile = from % 8;
        int toRank = to / 8, toFile = to % 8;

        int rankStep = Integer.compare(toRank, fromRank);
        int fileStep = Integer.compare(toFile, fromFile);

        int currentRank = fromRank + rankStep;
        int currentFile = fromFile + fileStep;

        while (currentRank != toRank || currentFile != toFile) {
            int square = currentRank * 8 + currentFile;
            if (board.isOccupied(square)) {
                return false;
            }
            currentRank += rankStep;
            currentFile += fileStep;
        }

        return true;
    }

    private static boolean isInCheck(ChessBoard board) {
        return isInCheck(board, board.isWhiteToMove());
    }

    private static boolean isInCheck(ChessBoard board, boolean whiteKing) {
        int kingValue = whiteKing ? 6 : 16;
        int kingPos = -1;
        for (int i = 0; i < 64; i++) {
            if (board.getPieceAt(i) == kingValue) {
                kingPos = i;
                break;
            }
        }

        if (kingPos == -1) return false;

        for (int from = 0; from < 64; from++) {
            if (!board.isOccupied(from)) continue;
            if (board.isWhitePiece(from) == whiteKing) continue;

            if (isBasicLegalMove(board, from, kingPos)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isGameOver(ChessBoard board) {
        return generateLegalMoves(board).isEmpty();
    }

    private static void applyMove(ChessBoard board, int from, int to) {
        board.setPieceAt(to, board.getPieceAt(from));
        board.setPieceAt(from, 0);
        board.setWhiteToMove(!board.isWhiteToMove());
    }

    private static ChessBoard copyBoard(ChessBoard original) {
        ChessBoard copy = new ChessBoard();
        for (int i = 0; i < 64; i++) {
            copy.setPieceAt(i, original.getPieceAt(i));
        }
        copy.setWhiteToMove(original.isWhiteToMove());
        return copy;
    }

    private record MoveCandidate(int moveIndex, float probability) {}

    private record AIMove(int moveIndex, float confidence, int alternativesConsidered) { }

    private enum GameResult {
        WHITE_WIN, BLACK_WIN, DRAW
    }
}
