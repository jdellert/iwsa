package de.jdellert.iwsa.changemodel;

import de.jdellert.iwsa.features.IpaFeatureTable;
import de.jdellert.iwsa.util.neural.NeuralModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.DataFormatException;

public class SoundChangeLogitModel extends NeuralModel {
    public SoundChangeLogitModel(String weightsDir, int numHiddenLayers, int hiddenSize, int inputDim) {
        super(weightsDir, numHiddenLayers, hiddenSize, inputDim);
    }

    public static SoundChangeLogitModel loadSoundChangeModel(String weightsDir) {
        return new SoundChangeLogitModel(weightsDir, 3, 128, 68);
    }

    public static SoundChangeLogitModel loadSoundChangeModel() {
        return loadSoundChangeModel("/de/jdellert/iwsa/neuralmodel/changemodel/weights");
    }

    public static void main(String[] args) {
        try {
            IpaFeatureTable featureTable = new IpaFeatureTable();
            SoundChangeLogitModel model = loadSoundChangeModel();
            int[] encodedA = featureTable.get("a");
            int[] encodedE = featureTable.get("e");
            System.out.println("encodedA: " + Arrays.toString(encodedA));
            System.out.println("encodedE: " + Arrays.toString(encodedE));
            String[] sounds = new String[]{"p", "t", "k", "a", "e", "i", "o", "u", "aː", "eː", "iː", "oː", "uː", "au", "ai", "ə", "-"};
            List<double[]> encodedPairs = new ArrayList<double[]>(sounds.length * sounds.length);
            for (String sound1 : sounds) {
                for (String sound2 : sounds) {
                    double[] encodedPair = featureTable.encodeDirectedPair(sound1, sound2);
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
    }
}
