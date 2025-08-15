package org.leycm.chessbot.model;

import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.leycm.chessbot.chess.ChessBoard;
import org.leycm.chessbot.chess.ChessPiece;

import java.io.*;

public class ReadOnlyChessModel {

    private static final String MODEL_PATH = "model/trained/chess_model-1.2.0-R0-FINAL.model";

    private Process engineProcess;
    private BufferedReader reader;
    private BufferedWriter writer;
    @Getter
    private boolean closed = false;

    @Contract(" -> new")
    public static @NotNull ReadOnlyChessModel loadNewest() {
        try {
            ModelLoader.loadModel(MODEL_PATH);
        } catch (IOException _) {}

        return new ReadOnlyChessModel();
    }

    private ReadOnlyChessModel() {
        ProcessBuilder builder = new ProcessBuilder(MODEL_PATH);

        try {
            engineProcess = builder.start();
            reader = new BufferedReader(new InputStreamReader(engineProcess.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(engineProcess.getOutputStream()));
            sendCommand("uci");
            waitForReady();
        } catch (IOException _) {
            closed = true;
        }

    }

    private void sendCommand(String cmd) throws IOException {
        writer.write(cmd + "\n");
        writer.flush();
    }

    private void waitForReady() throws IOException {
        sendCommand("isready");
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.equals("readyok")) break;
        }
    }

    public String findBestMove(String fen, int timeMillis) throws IOException {
        sendCommand("position fen " + fen);
        sendCommand("go movetime " + timeMillis);

        String line;
        String bestMove = null;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("bestmove")) {
                bestMove = line.split(" ")[1];
                break;
            }
        }
        return bestMove;
    }

    public void close() throws IOException {
        sendCommand("quit");
        reader.close();
        writer.close();
        engineProcess.destroy();

        closed = true;
    }

    public void makeBestMove(ChessBoard board) {
        try {
            String fen = boardToFEN(board);
            String bestMove = findBestMove(fen, 500); // 500ms pro Zug

            if (bestMove == null) {
                System.out.println("Kein Zug gefunden!");
                this.close();
                return;
            }

            int fromX = bestMove.charAt(0) - 'a';
            int toX = bestMove.charAt(2) - 'a';
            int fromRank = Character.getNumericValue(bestMove.charAt(1)) - 1;
            int toRank = Character.getNumericValue(bestMove.charAt(3)) - 1;
            int fromY = 7 - fromRank;
            int toY = 7 - toRank;

            System.out.printf("Bester Zug: %s -> von (%d,%d) nach (%d,%d)%n", bestMove, fromX, fromY, toX, toY);
            board.movePiece(fromX, fromY, toX, toY);

        } catch (IOException e) {
            System.out.println("An IO error have fun to debug Model closed");
            closed = true;
        }
    }

    private static @NotNull String boardToFEN(ChessBoard board) {
        StringBuilder fen = new StringBuilder();
        for (int y = 0; y < 8; y++) {
            int emptyCount = 0;
            for (int x = 0; x < 8; x++) {
                ChessPiece piece = board.getPiece(x, y);
                if (piece == null) {
                    emptyCount++;
                } else {
                    if (emptyCount > 0) {
                        fen.append(emptyCount);
                        emptyCount = 0;
                    }
                    char c = piece.getChar();
                    fen.append(piece.isWhite() ? Character.toUpperCase(c) : Character.toLowerCase(c));
                }
            }
            if (emptyCount > 0) fen.append(emptyCount);
            if (y < 7) fen.append("/");
        }
        fen.append(board.isWhiteTurn() ? " w " : " b ");
        fen.append("- - 0 1");
        return fen.toString();
    }
}
