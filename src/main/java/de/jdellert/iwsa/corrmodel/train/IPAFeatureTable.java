package de.jdellert.iwsa.corrmodel.train;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.DataFormatException;

public class IPAFeatureTable {
    private Map<String, int[]> featureTable;

    public IPAFeatureTable(String filepath) throws DataFormatException, IOException {
        featureTable = new HashMap<>();
        BufferedReader br = new BufferedReader(new FileReader(filepath));
        String line;
        while ((line = br.readLine()) != null) {
            String[] fields = line.split(",");
            if (fields[0].equals("")) {
                continue;
            }
            int[] features = new int[fields.length - 1];
            for (int i = 0; i < features.length; i++) {
                String feature = fields[i+1];
                /*int featureValue = switch (feature) {
                    case "+" -> 1;
                    case "-" -> -1;
                    case "0" -> 0;
                    default -> throw new DataFormatException();
                };*/
                int featureValue;
                switch (feature) {
                    case "+":
                        featureValue = 1;
                        break;
                    case "0":
                        featureValue = 0;
                        break;
                    case "-":
                        featureValue = -1;
                        break;
                    default:
                        throw new DataFormatException();
                }
                features[i] = featureValue;
            }
            featureTable.put(fields[0], features);
        }
        br.close();
    }

    public int[] get(String key) {
        return featureTable.get(key);
    }

    public int[] getPair(String sound1, String sound2) {
        int[] sound1Features = featureTable.get(sound1);
        int[] sound2Features = featureTable.get(sound2);

        if (sound1Features == null || sound2Features == null) {
            return null;
        }

        int[] soundPairFeatures = new int[sound1Features.length * 2];

        // concatenate feature arrays
        System.arraycopy(sound1Features, 0, soundPairFeatures, 0, sound1Features.length);
        System.arraycopy(sound2Features, 0, soundPairFeatures, sound1Features.length, sound2Features.length);

        return soundPairFeatures;
    }

    public Map<String, int[]> getFeatureTable() {
        return featureTable;
    }
}
