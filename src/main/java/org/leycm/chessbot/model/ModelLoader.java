package org.leycm.chessbot.model;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModelLoader {

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