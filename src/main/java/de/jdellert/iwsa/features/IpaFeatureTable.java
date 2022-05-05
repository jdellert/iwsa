package de.jdellert.iwsa.features;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.zip.DataFormatException;

public class IpaFeatureTable {
    private Map<String, int[]> featureTable;

    public IpaFeatureTable() throws DataFormatException, IOException {
        this("/de/jdellert/iwsa/features/all_ipa_symbols.csv");
    }

    public IpaFeatureTable(String filepath) throws DataFormatException, IOException {
        featureTable = new HashMap<>();
        BufferedReader br = new BufferedReader(new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream(filepath),"File "+filepath+" not found!"), StandardCharsets.UTF_8));
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
        int[] result = featureTable.get(key);
        if (result == null) {
            if (key.contains("̥")) return handleVoicelessDiacritic(key);
        }
        return result;
    }

    /**
     * Strips the voiceless diacritic from key for lookup, modifies voicedness feature in result.
     * Result is cached for more efficient lookup the next time.
     * @return the modified feature vector, which will be cached for easier retrieval in the future.
     */
    public int[] handleVoicelessDiacritic(String symbolWithvoicelessDiacritic) {
        String voicedEquivalent = symbolWithvoicelessDiacritic.replaceAll("̥", "");
        int[] voicedEquivalentVector = get(voicedEquivalent);
        if (voicedEquivalentVector == null) return null;
        int[] voicelessVector = Arrays.copyOf(voicedEquivalentVector, voicedEquivalentVector.length);
        voicelessVector[8] = -1;
        featureTable.put(symbolWithvoicelessDiacritic, voicelessVector);
        return voicelessVector;
    }
    
    public boolean contains(String key) {
    	return get(key) != null;
    }

    public double[] encodePair(String sound1, String sound2) {
        int[] sound1Features = get(sound1);
        int[] sound2Features = get(sound2);

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
