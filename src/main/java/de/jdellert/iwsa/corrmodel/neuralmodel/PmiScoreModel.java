package de.jdellert.iwsa.corrmodel.neuralmodel;

import de.jdellert.iwsa.features.IpaFeatureTable;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;

public class PmiScoreModel {
    private List<Layer> layers;

    public PmiScoreModel() {
        this("de/jdellert/iwsa/corrmodel/neuralmodel/weights", 3, 128, 34);
    }

    public PmiScoreModel(String weightsDir, int numHiddenLayers, int hiddenSize, int inputDim) {
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

    public static void main(String[] args) {
        try {
            IpaFeatureTable featureTable = new IpaFeatureTable();
            PmiScoreModel model = new PmiScoreModel();
        } catch (DataFormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        /*
        try {
            layers.add(new GeLULayer(128, 34, weightsDir + "first_layer_weights.txt",
                    weightsDir + "biases.txt"));
            layers.add(new GeLULayer(128, weightsDir + "second_layer_weights.txt",
                    weightsDir + "second_layer_biases.txt"));
            layers.add(new GeLULayer(128, weightsDir + "third_layer_weights.txt",
                    weightsDir + "third_layer_biases.txt"));
            layers.add(new Layer(1, 128, weightsDir + "final_layer_weights.txt",
                    weightsDir + "final_layer_biases.txt"));

            double[] test1 = {-1, -1, -1, -1, -1, -1, -1, 0, -1, -1, -1, 0, -1, 0, -1, -1, -1, -1, -1, -1, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            double[] test2 = {1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 0, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, -1, -1, -1, -1, -1, -1, -1, 0, 0, 0};
            double[][] test = new double[2][34];
            test[0] = test1;
            test[1] = test2;

            // double[][] tmp;
            for (Layer layer : layers) {
                test = layer.forward(test);
            }
            for (int i = 0; i < test.length; i++) {
                for (int j = 0; j < test[0].length; j++) {
                    System.out.println(test[i][j]);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }*/

    }
}
