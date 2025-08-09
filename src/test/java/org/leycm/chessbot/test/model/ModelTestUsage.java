package org.leycm.chessbot.test.model;

import org.leycm.chessbot.model.Model;
import org.leycm.chessbot.model.ModelLoader;
import org.leycm.chessbot.model.MoveEncoder;
import org.leycm.chessbot.trainer.ChessBoard;

import java.io.File;
import java.io.IOException;

public class ModelTestUsage {

    public static void main(String[] args) throws IOException {

        File modelFile = new File("model/trained/chess_bot-latest.model");
        if (!modelFile.exists()) {
            System.out.println("No trained model found. Please run Trainer first.");
            return;
        }

        Model model = ModelLoader.load(modelFile);
        System.out.println("Model loaded successfully");

        int[] board = ChessBoard.getCurrentBoard();

        float[] output = model.predict(board);
        int bestIndex = findBestIndex(output);
        int[] move = MoveEncoder.decodeMove(bestIndex);

        String fromSquare = MoveEncoder.squareToString(move[0]);
        String toSquare = MoveEncoder.squareToString(move[1]);

        System.out.println("Best move: " + fromSquare + " → " + toSquare);
        System.out.println("Confidence: " + output[bestIndex]);
    }

    private static int findBestIndex(float[] output) {
        int bestIndex = 0;
        float bestValue = output[0];

        for (int i = 1; i < output.length; i++) {
            if (output[i] > bestValue) {
                bestValue = output[i];
                bestIndex = i;
            }
        }

        return bestIndex;
    }
}
