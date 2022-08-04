package de.jdellert.iwsa.util.neural;

import java.util.ArrayList;
import java.util.List;

public class NeuralModel {
    private List<Layer> layers;

    public NeuralModel(String weightsDir, int numHiddenLayers, int hiddenSize, int inputDim) {
        layers = new ArrayList<>();

        try {
            for (int i = 0; i < numHiddenLayers; i++) {
                String layerWeightsDir = weightsDir + "/layer" + (i+1);
                // need to specify inputDim only for first hidden layer
                if (i == 0) {
                    layers.add(new GeLULayer(hiddenSize, inputDim, layerWeightsDir + "/weights.txt",
                            layerWeightsDir + "/biases.txt"));
                } else {
                    layers.add(new GeLULayer(hiddenSize, layerWeightsDir + "/weights.txt",
                            layerWeightsDir + "/biases.txt"));
                }
            }
            // add final layer
            String lastLayerDir = weightsDir + "/layer" + (numHiddenLayers+1);
            layers.add(new Layer(1, 128, lastLayerDir + "/weights.txt",
                    lastLayerDir + "/biases.txt"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public double[] predict(double[][] x) {
        for (Layer layer : layers) {
            x = layer.forward(x);
        }
        double[] results = new double[x.length];
        for (int i = 0; i < x.length; i++) {
            results[i] = x[i][0];
        }
        return results;
    }

    public double predict(double[] x) {
        return predict(new double[][]{x})[0];
    }
}
