package org.leycm.chessbot.model;

import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.leycm.chessbot.chess.ChessMove;

@Getter
public class ChessModel {
    private final DenseLayer[] layers;
    private final int inputSize = 65;
    private final int outputSize = 4096;
    private final int[] hiddenSizes = {512, 256, 128, 64, 128, 256, 512, 256, 128};

    public ChessModel() {
        layers = new DenseLayer[hiddenSizes.length + 1];

        int prevSize = inputSize;
        for (int i = 0; i < hiddenSizes.length; i++) {
            layers[i] = new DenseLayer(prevSize, hiddenSizes[i]);
            prevSize = hiddenSizes[i];
        }

        layers[hiddenSizes.length] = new DenseLayer(prevSize, outputSize);
    }

    public double[] predict(int @NotNull [] boardState) {
        double[] input = new double[inputSize];
        for (int i = 0; i < boardState.length; i++) {
            input[i] = boardState[i] / 10.0;
        }

        double[] current = input;
        for (DenseLayer layer : layers) {
            current = layer.forward(current);
        }

        return current;
    }

    public void train(int @NotNull [] boardState, ChessMove move, double reward) {
        double[] input = new double[inputSize];
        for (int i = 0; i < boardState.length; i++) {
            input[i] = boardState[i] / 10.0;
        }

        double[] target = new double[outputSize];
        int moveIndex = moveToIndex(move);
        if (moveIndex >= 0 && moveIndex < outputSize) {
            target[moveIndex] = reward;
        }

        double[] predicted = predict(boardState);
        double[] error = new double[outputSize];
        for (int i = 0; i < outputSize; i++) {
            error[i] = target[i] - predicted[i];
        }

        backpropagate(input, error);
    }

    private void backpropagate(double[] input, double[] error) {
        double[] currentError = error;

        for (int i = layers.length - 1; i >= 0; i--) {
            currentError = layers[i].backward(currentError, 0.001);
        }
    }

    @Contract(pure = true)
    private int moveToIndex(ChessMove move) {
        if (move == null || move.isValid()) return -1;
        return move.getFromX() * 512 + move.getFromX() * 64 + move.getToX() * 8 + move.getToY();
    }

}