package org.leycm.chessbot.model;

import org.jetbrains.annotations.NotNull;
import org.leycm.chessbot.chess.ChessBoard;
import org.leycm.chessbot.chess.ChessPiece;

import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModelLoader {

    public static void makeBestMove(ChessBoard board) throws Exception {
        String fen = boardToFEN(board);
        String encodedFen = URLEncoder.encode(fen, StandardCharsets.UTF_8);

        String url = "https://lichess.org/api/cloud-eval?fen=" + encodedFen;
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String json = response.body();

        int idx = json.indexOf("\"moves\":\"");
        if (idx == -1) {
            System.out.println("Kein Zug gefunden");
            return;
        }
        String movesPart = json.substring(idx + 9);
        String bestMove = movesPart.split("\"")[0].split(" ")[0];

        int fromX = bestMove.charAt(0) - 'a';
        int fromY = 8 - Character.getNumericValue(bestMove.charAt(1));
        int toX = bestMove.charAt(2) - 'a';
        int toY = 8 - Character.getNumericValue(bestMove.charAt(3));

        board.movePiece(fromX, fromY, toX, toY);
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
                    char c = piece.getColorChar();
                    fen.append(piece.isWhite() ? Character.toUpperCase(c) : Character.toLowerCase(c));
                }
            }
            if (emptyCount > 0) {
                fen.append(emptyCount);
            }
            if (y < 7) fen.append("/");
        }

        fen.append(board.isWhiteTurn() ? " w " : " b ");
        fen.append("- - 0 1");
        return fen.toString();
    }

    public static void saveModel(@NotNull ChessModel model, String filename) throws IOException {
        try (DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(filename)))) {

            DenseLayer[] layers = model.getLayers();
            dos.writeInt(layers.length);

            for (DenseLayer layer : layers) {
                double[][] weights = layer.getWeights();
                double[] biases = layer.getBiases();

                dos.writeInt(weights.length);
                dos.writeInt(weights[0].length);

                for (double[] weightRow : weights) {
                    for (double weight : weightRow) {
                        dos.writeDouble(weight);
                    }
                }

                for (double bias : biases) {
                    dos.writeDouble(bias);
                }
            }
        }
    }

    public static @NotNull ChessModel loadModel(String filename) throws IOException {
        if (!Files.exists(Path.of(filename))) {
            return new ChessModel();
        }

        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(new FileInputStream(filename)))) {

            ChessModel model = new ChessModel();
            DenseLayer[] layers = model.getLayers();

            int layerCount = dis.readInt();

            for (int i = 0; i < layerCount && i < layers.length; i++) {
                int outputSize = dis.readInt();
                int inputSize = dis.readInt();

                double[][] weights = layers[i].getWeights();
                double[] biases = layers[i].getBiases();

                for (int j = 0; j < Math.min(outputSize, weights.length); j++) {
                    for (int k = 0; k < Math.min(inputSize, weights[j].length); k++) {
                        weights[j][k] = dis.readDouble();
                    }
                }

                for (int j = 0; j < Math.min(outputSize, biases.length); j++) {
                    biases[j] = dis.readDouble();
                }
            }

            return model;
        }
    }
}