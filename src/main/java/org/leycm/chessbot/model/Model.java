package org.leycm.chessbot.model;

import org.jetbrains.annotations.NotNull;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class Model {
    private final DenseLayer[] layers;
    private final float learningRate;
    private final int[] layerSizes;
    private static final ForkJoinPool threadPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors() * 2);

    public Model(int[] layerSizes, float learningRate) {
        this(layerSizes, learningRate, false);
    }

    public Model(int @NotNull [] layerSizes, float learningRate, boolean useGPU) {
        this.learningRate = learningRate;
        this.layerSizes = layerSizes.clone();
        this.layers = new DenseLayer[layerSizes.length - 1];

        if (layerSizes.length > 3) {
            threadPool.invoke(new LayerInitTask(0, layers.length));
        } else {
            for (int i = 0; i < layers.length; i++) {
                layers[i] = new DenseLayer(layerSizes[i], layerSizes[i + 1], useGPU);
            }
        }
    }

    private class LayerInitTask extends RecursiveAction {
        private final int start;
        private final int end;

        LayerInitTask(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        protected void compute() {
            if (end - start <= 2) {
                for (int i = start; i < end; i++) {
                    layers[i] = new DenseLayer(layerSizes[i], layerSizes[i + 1], false);
                }
            } else {
                int mid = (start + end) / 2;
                invokeAll(new LayerInitTask(start, mid),
                         new LayerInitTask(mid, end));
            }
        }
    }

    public float[] predict(int @NotNull [] input) {
        float[] current = new float[input.length];
        
        if (input.length > 256) {
            threadPool.invoke(new NormalizeTask(input, current, 0, input.length));
        } else {
            for (int i = 0; i < input.length; i++) {
                current[i] = input[i] / 16.0f;
            }
        }

        for (DenseLayer layer : layers) {
            current = layer.forward(current);
        }

        return current;
    }

    private class NormalizeTask extends RecursiveAction {
        private final int[] input;
        private final float[] output;
        private final int start;
        private final int end;

        NormalizeTask(int[] input, float[] output, int start, int end) {
            this.input = input;
            this.output = output;
            this.start = start;
            this.end = end;
        }

        @Override
        protected void compute() {
            if (end - start <= 64) {
                for (int i = start; i < end; i++) {
                    output[i] = input[i] / 16.0f;
                }
            } else {
                int mid = (start + end) / 2;
                invokeAll(new NormalizeTask(input, output, start, mid),
                         new NormalizeTask(input, output, mid, end));
            }
        }
    }

    public void trainBatch(int[][] inputs, int[] targets, int batchSize) {
        float totalLoss = 0f;

        if (batchSize > 512) {
            threadPool.invoke(new BatchTrainingTask(inputs, targets, batchSize, 0, batchSize));
        } else {
            for (int i = 0; i < batchSize; i++) {
                float[] expected = new float[4096];
                expected[targets[i]] = 1.0f;

                float[] predicted = predict(inputs[i]);

                float loss = 0f;
                for (int j = 0; j < predicted.length; j++) {
                    loss += expected[j] * (float)Math.log(predicted[j] + 1e-8f);
                }
                totalLoss -= loss;

                float[] outputGrad = new float[predicted.length];
                for (int j = 0; j < outputGrad.length; j++) {
                    outputGrad[j] = predicted[j] - expected[j];
                }

                float[] currentGrad = outputGrad;
                for (int l = layers.length - 1; l >= 0; l--) {
                    currentGrad = layers[l].backwardAccumulate(currentGrad);
                }
            }
        }

        if (layers.length > 2) {
            threadPool.invoke(new WeightUpdateTask(0, layers.length, batchSize));
        } else {
            for (DenseLayer layer : layers) {
                layer.updateWeights(learningRate, batchSize);
            }
        }

        if (Math.random() < 0.01) {
            System.out.printf(" -> Batch loss: %.4f%n", totalLoss / batchSize);
        }
    }

    private class BatchTrainingTask extends RecursiveAction {
        private final int[][] inputs;
        private final int[] targets;
        private final int batchSize;
        private final int start;
        private final int end;

        BatchTrainingTask(int[][] inputs, int[] targets, int batchSize, int start, int end) {
            this.inputs = inputs;
            this.targets = targets;
            this.batchSize = batchSize;
            this.start = start;
            this.end = end;
        }

        @Override
        protected void compute() {
            if (end - start <= 128) {
                for (int i = start; i < end; i++) {
                    float[] expected = new float[4096];
                    expected[targets[i]] = 1.0f;

                    float[] predicted = predict(inputs[i]);

                    float[] outputGrad = new float[predicted.length];
                    for (int j = 0; j < outputGrad.length; j++) {
                        outputGrad[j] = predicted[j] - expected[j];
                    }

                    float[] currentGrad = outputGrad;
                    for (int l = layers.length - 1; l >= 0; l--) {
                        currentGrad = layers[l].backwardAccumulate(currentGrad);
                    }
                }
            } else {
                int mid = (start + end) / 2;
                invokeAll(new BatchTrainingTask(inputs, targets, batchSize, start, mid),
                         new BatchTrainingTask(inputs, targets, batchSize, mid, end));
            }
        }
    }

    private class WeightUpdateTask extends RecursiveAction {
        private final int start;
        private final int end;
        private final int batchSize;

        WeightUpdateTask(int start, int end, int batchSize) {
            this.start = start;
            this.end = end;
            this.batchSize = batchSize;
        }

        @Override
        protected void compute() {
            if (end - start <= 2) {
                for (int i = start; i < end; i++) {
                    layers[i].updateWeights(learningRate, batchSize);
                }
            } else {
                int mid = (start + end) / 2;
                invokeAll(new WeightUpdateTask(start, mid, batchSize),
                         new WeightUpdateTask(mid, end, batchSize));
            }
        }
    }

    @Deprecated
    public void train(int[] input, float @NotNull [] expected) {
        int[][] inputs = {input};
        int targetIndex = 0;
        for (int i = 0; i < expected.length; i++) {
            if (expected[i] > 0.5f) {
                targetIndex = i;
                break;
            }
        }
        int[] targets = {targetIndex};
        trainBatch(inputs, targets, 1);
    }

    public DenseLayer[] getLayers() {
        return layers;
    }
}

