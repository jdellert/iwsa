package de.jdellert.iwsa.corrmodel.neuralmodel;

import de.jdellert.iwsa.features.IpaFeatureTable;
import de.jdellert.iwsa.util.neural.NeuralModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.DataFormatException;

public class PmiScoreModel extends NeuralModel {
    public PmiScoreModel(String weightsDir, int numHiddenLayers, int hiddenSize, int inputDim) {
        super(weightsDir, numHiddenLayers, hiddenSize, inputDim);
    }

    public static PmiScoreModel loadPairwiseNeuralModel() {
        return new PmiScoreModel("/de/jdellert/iwsa/neuralmodel/corrmodel/weights", 3, 128, 34);
    }

    public static PmiScoreModel loadGapModel() {
        return new PmiScoreModel("/de/jdellert/iwsa/neuralmodel/gapmodel/weights", 3, 128, 34);
    }

    public static void main(String[] args) {
        try {
            IpaFeatureTable featureTable = new IpaFeatureTable();
            PmiScoreModel model = loadPairwiseNeuralModel();
            int[] encodedA = featureTable.get("a");
            int[] encodedE = featureTable.get("e");
            System.out.println("encodedA: " + Arrays.toString(encodedA));
            System.out.println("encodedE: " + Arrays.toString(encodedE));
            String[] sounds = new String[] {"p", "t", "k", "a", "e", "i", "o", "u", "aː", "eː", "iː", "oː", "uː","au", "ai", "ə", "-"};
            List<double[]> encodedPairs = new ArrayList<double[]>(sounds.length * sounds.length);
            for (String sound1 : sounds) {
                for (String sound2: sounds) {
                    double[] encodedPair = featureTable.encodePair(sound1, sound2);
                    encodedPairs.add(encodedPair);
                }
            }
            double[][] inputs = encodedPairs.toArray(new double[encodedPairs.size()][]);
            double[] predictions = model.predict(inputs);
            int idx = 0;
            for (String sound1 : sounds) {
                for (String sound2 : sounds) {
                    System.err.println(sound1 + "\t" + sound2 + "\t" + predictions[idx++]);
                }
            }
        } catch (DataFormatException | IOException e) {
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
