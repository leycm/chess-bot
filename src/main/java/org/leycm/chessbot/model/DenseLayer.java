package org.leycm.chessbot.model;


import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class DenseLayer {
    private float[][] weights;
    private float[] biases;
    private float[][] weightGradients;
    private float[] biasGradients;
    private float[] lastInput;
    private float[] lastOutput;
    private boolean useGPU;
    private static final ForkJoinPool threadPool = new ForkJoinPool();

    public DenseLayer(int inputSize, int outputSize) {
        this(inputSize, outputSize, false);
    }

    public DenseLayer(int inputSize, int outputSize, boolean useGPU) {
        this.weights = new float[outputSize][inputSize];
        this.biases = new float[outputSize];
        this.weightGradients = new float[outputSize][inputSize];
        this.biasGradients = new float[outputSize];
        this.useGPU = useGPU;

        Random rand = new Random();
        float scale = (float) Math.sqrt(2.0 / inputSize);

        for (int i = 0; i < outputSize; i++) {
            biases[i] = 0;
            for (int j = 0; j < inputSize; j++) {
                weights[i][j] = (rand.nextFloat() - 0.5f) * 2 * scale;
            }
        }
    }

    public float[] forward(float[] input) {
        this.lastInput = input.clone();
        float[] output = new float[weights.length];

        if (useGPU || weights.length > 256) {
            // Use parallel computation for large layers
            threadPool.invoke(new ForwardTask(input, output, 0, output.length));
        } else {
            // Sequential for small layers
            for (int i = 0; i < output.length; i++) {
                output[i] = biases[i];
                for (int j = 0; j < input.length; j++) {
                    output[i] += weights[i][j] * input[j];
                }
                output[i] = relu(output[i]);
            }
        }

        this.lastOutput = output.clone();
        return output;
    }

    private class ForwardTask extends RecursiveAction {
        private final float[] input;
        private final float[] output;
        private final int start;
        private final int end;

        ForwardTask(float[] input, float[] output, int start, int end) {
            this.input = input;
            this.output = output;
            this.start = start;
            this.end = end;
        }

        @Override
        protected void compute() {
            if (end - start <= 64) {
                for (int i = start; i < end; i++) {
                    output[i] = biases[i];
                    for (int j = 0; j < input.length; j++) {
                        output[i] += weights[i][j] * input[j];
                    }
                    output[i] = relu(output[i]);
                }
            } else {
                int mid = (start + end) / 2;
                invokeAll(new ForwardTask(input, output, start, mid),
                        new ForwardTask(input, output, mid, end));
            }
        }
    }

    public float[] backwardAccumulate(float[] outputGrad) {
        float[] inputGrad = new float[lastInput.length];

        // Apply ReLU derivative
        for (int i = 0; i < outputGrad.length; i++) {
            if (lastOutput[i] <= 0) {
                outputGrad[i] = 0;
            }
        }

        if (useGPU || weights.length > 256) {
            // Parallel gradient computation
            threadPool.invoke(new BackwardTask(outputGrad, inputGrad, 0, inputGrad.length));
        } else {
            // Sequential
            for (int j = 0; j < inputGrad.length; j++) {
                for (int i = 0; i < outputGrad.length; i++) {
                    inputGrad[j] += weights[i][j] * outputGrad[i];
                }
            }
        }

        // Accumulate gradients instead of applying immediately
        for (int i = 0; i < weights.length; i++) {
            biasGradients[i] += outputGrad[i];
            for (int j = 0; j < weights[i].length; j++) {
                weightGradients[i][j] += outputGrad[i] * lastInput[j];
            }
        }

        return inputGrad;
    }

    private class BackwardTask extends RecursiveAction {
        private final float[] outputGrad;
        private final float[] inputGrad;
        private final int start;
        private final int end;

        BackwardTask(float[] outputGrad, float[] inputGrad, int start, int end) {
            this.outputGrad = outputGrad;
            this.inputGrad = inputGrad;
            this.start = start;
            this.end = end;
        }

        @Override
        protected void compute() {
            if (end - start <= 64) {
                for (int j = start; j < end; j++) {
                    for (int i = 0; i < outputGrad.length; i++) {
                        inputGrad[j] += weights[i][j] * outputGrad[i];
                    }
                }
            } else {
                int mid = (start + end) / 2;
                invokeAll(new BackwardTask(outputGrad, inputGrad, start, mid),
                        new BackwardTask(outputGrad, inputGrad, mid, end));
            }
        }
    }

    public void updateWeights(float learningRate, int batchSize) {
        float lr = learningRate / batchSize;

        for (int i = 0; i < weights.length; i++) {
            biases[i] -= lr * biasGradients[i];
            biasGradients[i] = 0; // Reset

            for (int j = 0; j < weights[i].length; j++) {
                weights[i][j] -= lr * weightGradients[i][j];
                weightGradients[i][j] = 0; // Reset
            }
        }
    }

    @Deprecated
    public float[] backward(float[] outputGrad, float learningRate) {
        float[] inputGrad = backwardAccumulate(outputGrad);
        updateWeights(learningRate, 1);
        return inputGrad;
    }

    private float relu(float x) {
        return Math.max(0, x);
    }

    public float[][] getWeights() {
        return weights;
    }

    public float[] getBiases() {
        return biases;
    }

    public void setWeights(float[][] weights) {
        this.weights = weights;
    }

    public void setBiases(float[] biases) {
        this.biases = biases;
    }
}