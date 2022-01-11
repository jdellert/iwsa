package de.jdellert.iwsa.features;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.DataFormatException;

public class IpaFeatureTable {
    private Map<String, int[]> featureTable;

    public IpaFeatureTable() throws DataFormatException, IOException {
        this("iwsa/src/main/resources/de/jdellert/iwsa/features/all_ipa_symbols.csv");
    }

    public IpaFeatureTable(String filepath) throws DataFormatException, IOException {
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
    
    public boolean contains(String key) {
    	return featureTable.containsKey(key);
    }

    public double[] encodePair(String sound1, String sound2) {
        int[] sound1Features = featureTable.get(sound1);
        int[] sound2Features = featureTable.get(sound2);

        if (sound1Features == null) {
           System.err.println("ERROR: IPA Symbol " + sound1 + " was not defined in feature table");
            return null;
        }
        else if (sound2Features == null) {
            System.err.println("ERROR: IPA Symbol " + sound2 + " was not defined in feature table");
            return null;
        }

        double[] soundPairFeatures = new double[sound1Features.length];

        for (int i = 0; i < sound1Features.length; i++) {
            if ((sound1Features[i] == 1 && sound2Features[i] == 1) || (sound1Features[i] == -1 && sound2Features[i] == -1)) {
                soundPairFeatures[i] = 1;
            } else if (sound1Features[i] == 0 && sound2Features[i] == 0) {
                soundPairFeatures[i] = 0;
            } else {
                soundPairFeatures[i] = -1;
            }
        }

        return soundPairFeatures;
    }

    public Map<String, int[]> getFeatureTable() {
        return featureTable;
    }
}
