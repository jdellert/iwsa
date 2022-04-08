package de.jdellert.iwsa.features;

import java.io.*;
import java.util.*;
import java.util.zip.DataFormatException;

public class IpaFeatureTable {
    private final Map<String, int[]> featureTable;
    private ArrayList<String> features;
    private final Map<String, String[]> modifierTable;

    public IpaFeatureTable() throws DataFormatException, IOException {
        this("iwsa/src/main/resources/de/jdellert/iwsa/features/all_ipa_symbols.csv");
    }

    public IpaFeatureTable(String filepath) throws DataFormatException, IOException {
        this(filepath, "iwsa/src/main/resources/de/jdellert/iwsa/features/diacritic_rules.csv");
    }

    public IpaFeatureTable(String filepath, String modifierFilepath) throws DataFormatException, IOException {
        featureTable = new HashMap<>();
        BufferedReader br = new BufferedReader(new FileReader(filepath));
        String line;
        boolean firstLine = true;
        while ((line = br.readLine()) != null) {
            String[] fields = line.split(",");
            if (firstLine) {
                features = new ArrayList<>(Arrays.asList(fields));
                features.remove(0); // remove empty string from index 0
                firstLine = false;
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

        br = new BufferedReader(new FileReader(modifierFilepath));
        modifierTable = new HashMap<>();

        while ((line = br.readLine()) != null) {
            String[] fields = line.split("\\s+");
            String modSymbol = fields[0];
            try {
                String[] modifications = fields[1].split(",");
                modifierTable.put(modSymbol, modifications);
            } catch (ArrayIndexOutOfBoundsException e) {
                // add empty array for symbols that don't change any feature
                modifierTable.put(modSymbol, new String[]{""});
            }
        }

        br.close();
    }

    public int[] get(String key) {
        /*
        int[] result = featureTable.get(key);
        if (result == null) {
            if (key.contains("̥")) return handleVoicelessDiacritic(key);
        }
        return result;*/
        return handleDiacritic(key);
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

    /**
     * Strips trailing or leading diacritic from key and modifies respective feature(s).
     * Recursion allows for handling multiple diacritics in arbitrary order.
     * Result is cached for more efficient lookup the next time.
     * @param symbolWithDiacritic key for lookup
     * @return the modified feature vector
     */
    public int[] handleDiacritic(String symbolWithDiacritic) {
        if (featureTable.containsKey(symbolWithDiacritic)) {
            return featureTable.get(symbolWithDiacritic);
        } else {
        	if (symbolWithDiacritic.length() == 1) {
        		return null;
        	}
        	
            // split off last char, try to handle it as a modifier
            int lastCharIdx = symbolWithDiacritic.length() - 1;
            String modifier = symbolWithDiacritic.substring(lastCharIdx);
            String remainingSymbol = symbolWithDiacritic.substring(0, lastCharIdx);

            String[] modifications = modifierTable.get(modifier);

            // if last char is not a valid modifier, try first char (e.g. for pre-aspiration)
            if (modifications == null) {
                modifier = symbolWithDiacritic.substring(0, 1);
                remainingSymbol = symbolWithDiacritic.substring(1);
                modifications = modifierTable.get(modifier);

                if (modifications == null) {
                    return null;
                }
            }

            int[] featureVector = handleDiacritic(remainingSymbol);

            if (featureVector == null) {
                return null;
            }

            featureVector = Arrays.copyOf(featureVector, featureVector.length);

            for (String mod : modifications) {
                if (mod.equals("")) {
                    continue;
                }
                String modifiedFeature = mod.substring(1);
                int featureIdx = features.indexOf(modifiedFeature);
                if (mod.charAt(0) == '+') {
                    featureVector[featureIdx] = 1;
                } else if (mod.charAt(0) == '-') {
                    featureVector[featureIdx] = -1;
                } else {
                    System.err.println("ERROR: File for sound modifications is malformed. Rule " + mod +
                            " cannot be applied to " + symbolWithDiacritic + ".");
                    return null;
                }
            }

            featureTable.put(remainingSymbol, featureVector);
            return featureVector;
        }
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
