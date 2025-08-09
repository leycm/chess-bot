package org.leycm.chessbot.model;


import java.io.*;

public class ModelLoader {

    public static void save(Model model, File file) throws IOException {
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(file))) {
            DenseLayer[] layers = model.getLayers();
            dos.writeInt(layers.length);

            for (DenseLayer layer : layers) {
                float[][] weights = layer.getWeights();
                float[] biases = layer.getBiases();

                dos.writeInt(weights[0].length);
                dos.writeInt(weights.length);

                for (float[] weight : weights) {
                    for (float v : weight) {
                        dos.writeFloat(v);
                    }
                }

                for (float bias : biases) {
                    dos.writeFloat(bias);
                }
            }
        }
    }

    public static Model load(File file) throws IOException {
        try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
            int layerCount = dis.readInt();
            DenseLayer[] layers = new DenseLayer[layerCount];

            int[] layerSizes = new int[layerCount + 1];

            for (int l = 0; l < layerCount; l++) {
                int inputSize = dis.readInt();
                int outputSize = dis.readInt();

                if (l == 0) layerSizes[0] = inputSize;
                layerSizes[l + 1] = outputSize;

                float[][] weights = new float[outputSize][inputSize];
                float[] biases = new float[outputSize];

                for (int i = 0; i < outputSize; i++) {
                    for (int j = 0; j < inputSize; j++) {
                        weights[i][j] = dis.readFloat();
                    }
                }

                for (int i = 0; i < outputSize; i++) {
                    biases[i] = dis.readFloat();
                }

                layers[l] = new DenseLayer(inputSize, outputSize);
                layers[l].setWeights(weights);
                layers[l].setBiases(biases);
            }

            Model model = new Model(layerSizes, 0.001f);

            for (int i = 0; i < layers.length; i++) {
                model.getLayers()[i] = layers[i];
            }

            return model;
        }
    }
}
