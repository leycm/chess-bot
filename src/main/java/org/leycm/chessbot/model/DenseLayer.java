package org.leycm.chessbot.model;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class DenseLayer {
    private final double[][] weights;
    private final double[] biases;
    private double[] lastInput;
    private double[] lastOutput;

    public DenseLayer(int inputSize, int outputSize) {
        weights = new double[outputSize][inputSize];
        biases = new double[outputSize];
        initializeWeights(inputSize, outputSize);
    }

    private void initializeWeights(int inputSize, int outputSize) {
        Random random = ThreadLocalRandom.current();
        double limit = Math.sqrt(6.0 / (inputSize + outputSize));

        for (int i = 0; i < outputSize; i++) {
            for (int j = 0; j < inputSize; j++) {
                weights[i][j] = (random.nextDouble() * 2 - 1) * limit;
            }
            biases[i] = 0.0;
        }
    }

    public double[] forward(double[] input) {
        this.lastInput = input.clone();
        double[] output = new double[weights.length];

        for (int i = 0; i < weights.length; i++) {
            double sum = biases[i];
            for (int j = 0; j < input.length; j++) {
                sum += weights[i][j] * input[j];
            }
            output[i] = relu(sum);
        }

        this.lastOutput = output;
        return output;
    }

    public double[] backward(double[] gradients, double learningRate) {
        double[] inputGradients = new double[lastInput.length];

        for (int i = 0; i < weights.length; i++) {
            double gradient = gradients[i] * reluDerivative(lastOutput[i]);

            biases[i] += learningRate * gradient;

            for (int j = 0; j < lastInput.length; j++) {
                inputGradients[j] += weights[i][j] * gradient;
                weights[i][j] += learningRate * gradient * lastInput[j];
            }
        }

        return inputGradients;
    }

    private double relu(double x) {
        return Math.max(0, x);
    }

    private double reluDerivative(double x) {
        return x > 0 ? 1.0 : 0.0;
    }

    public double[][] getWeights() {
        return weights;
    }

    public double[] getBiases() {
        return biases;
    }
}

