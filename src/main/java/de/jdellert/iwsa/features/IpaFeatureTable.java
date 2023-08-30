package de.jdellert.iwsa.features;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.DataFormatException;

public class IpaFeatureTable {
    // private final Map<String, int[]> featureTable;
    public Map<String, int[]> featureTable;
    public ArrayList<String> features;
    private ArrayList<String> metasymbols;
    private final Map<String, String[]> modifierTable;
    private final Map<String, double[]> vowelDimensions;

    public IpaFeatureTable() throws DataFormatException, IOException {
        this("/de/jdellert/iwsa/features/all_ipa_symbols.csv");
    }

    public IpaFeatureTable(String filepath) throws DataFormatException, IOException {
        this(filepath, "/de/jdellert/iwsa/features/diacritic_rules.csv");
    }

    public IpaFeatureTable(String filepath, String modifierFilepath) throws DataFormatException, IOException {
        featureTable = new HashMap<>();
        metasymbols = new ArrayList<>();
        BufferedReader br = new BufferedReader(new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream(filepath),"File "+filepath+" not found!"), StandardCharsets.UTF_8));
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

        br = new BufferedReader(new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream(modifierFilepath),"File "+modifierFilepath+" not found!"), StandardCharsets.UTF_8));
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

        String vowelDimFile = "/de/jdellert/iwsa/features/vowel_dimensions.csv";
        br = new BufferedReader(new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream(vowelDimFile),"File "+vowelDimFile+" not found!"), StandardCharsets.UTF_8));
        vowelDimensions = new HashMap<>();

        while ((line = br.readLine()) != null) {
            String[] fields = line.split("\\s+");
            String vowel = fields[0];
            String[] dimensionsAsString = fields[1].split(",");
            double[] dimensions = new double[dimensionsAsString.length];

            for (int i = 0; i < dimensionsAsString.length; i++) {
                dimensions[i] = Double.parseDouble(dimensionsAsString[i]);
            }

            vowelDimensions.put(vowel, dimensions);
        }

        br.close();
    }

    public static IpaFeatureTable createFeatureTable() {
        try {
            return new IpaFeatureTable();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public int[] get(String key) {
        /*
        int[] result = featureTable.get(key);
        if (result == null) {
            if (key.contains("̥")) return handleVoicelessDiacritic(key);
        }
        return result;*/
        if (getVowelCount(key) > 1) {
            String reorderedPolyphthong = handlePolyphthong(key);
            if (reorderedPolyphthong == null) {
                System.err.println("ERROR: Tried to encode " + key + " as polyphthong and failed.");
                return null;
            }
            key = reorderedPolyphthong;
        }
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
    private int[] handleDiacritic(String symbolWithDiacritic) {
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

            int[] baseFeatureVector = handleDiacritic(remainingSymbol);

            if (baseFeatureVector == null) {
                return null;
            }

            int[] featureVector = Arrays.copyOf(baseFeatureVector, baseFeatureVector.length);

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

            featureTable.put(symbolWithDiacritic, featureVector);
            return featureVector;
        }
    }

    /**
     * Encodes diphthongs by adding / modifying relevant features.
     * The resulting feature vector is stored for more efficient lookup.
     * @param diphthong the diphthong in its clean form (ONLY the two vowels, no diacritics etc.)
     * @return the resulting feature vector
     */
    private int[] encodeDiphthong(String diphthong) {
        if (diphthong.length() != 2) {
            System.err.println("Unable to encode " + diphthong + " as diphthong!");
            return null;
        }

        /*
        relevant features with indices:
        backshift: 24
        frontshift: 25
        opening: 26
        centering: 27
        closing: 28
        longdistance: 29
        secondrounded: 30
         */

        String firstVowel = diphthong.substring(0, 1);
        String secondVowel = diphthong.substring(1);
        double[] firstVowelDimensions = vowelDimensions.get(firstVowel);
        double[] secondVowelDimensions = vowelDimensions.get(secondVowel);
        // vowel dimension are ordered triplets indicating the horizontal [0] and vertical [1] position
        // of the vowel in the mouth, as well as its roundedness [2].
        // a low first value indicates a front vowel, a low second value indicates a closed vowel.
        // roundedness is encoded with 1 (rounded) or 0 (unrounded).

        int[] baseFeatureVector = featureTable.get(firstVowel);

        if (firstVowelDimensions == null || secondVowelDimensions == null || baseFeatureVector == null) {
            System.err.println("Unable to encode " + diphthong + " as diphthong!");
            return null;
        }

        int[] featureVector = Arrays.copyOf(baseFeatureVector, baseFeatureVector.length);

        // backshift / frontshift?
        if (secondVowelDimensions[0] > firstVowelDimensions[0]) { // backshift
            featureVector[24] = 1;
            featureVector[25] = -1;
        } else if (secondVowelDimensions[0] < firstVowelDimensions[0]) { // frontshift
            featureVector[24] = -1;
            featureVector[25] = 1;
        } else {
            featureVector[24] = -1;
            featureVector[25] = -1;
        }

        // opening / closing?
        if (Math.abs(firstVowelDimensions[1] - secondVowelDimensions[1]) > 0.8) {
            if (secondVowelDimensions[1] > firstVowelDimensions[1]) { // opening
                featureVector[26] = 1;
                featureVector[28] = -1;
            } else { // closing
                featureVector[26] = -1;
                featureVector[28] = 1;
            }
        } else {
            featureVector[26] = -1;
            featureVector[28] = -1;
        }

        // centering?
        if ((secondVowelDimensions[0] == 2 && 2 <= secondVowelDimensions[1] && secondVowelDimensions[1] <= 3) ||
                (secondVowelDimensions[1] == 1.5 && firstVowelDimensions[1] != 1.5) ||
                (secondVowelDimensions[1] == 3.5 && firstVowelDimensions[1] != 3.5) ||
                (secondVowelDimensions[1] == 2 && firstVowelDimensions[1] < 2) ||
                (secondVowelDimensions[1] == 3 && firstVowelDimensions[1] > 3)) {
            featureVector[27] = 1;
        } else {
            featureVector[27] = -1;
        }

        // longdistance?
        if (2 <= firstVowelDimensions[1] && firstVowelDimensions[1] <= 3) {
            if (Math.abs(firstVowelDimensions[1] - secondVowelDimensions[1]) == 2) {
                featureVector[29] = 1;
            } else {
                featureVector[29] = -1;
            }
        } else {
            if (Math.abs(firstVowelDimensions[1] - secondVowelDimensions[1]) > 2) {
                featureVector[29] = 1;
            } else {
                featureVector[29] = -1;
            }
        }

        // secondrounded?
        if (secondVowelDimensions[2] == 1) {
            featureVector[30] = 1;
        } else {
            featureVector[30] = -1;
        }

        featureTable.put(diphthong, featureVector);

        return featureVector;
    }

    /**
     * Encodes triphthongs by adding / modifying relevant features.
     * The resulting feature vector is stored for more efficient lookup.
     * @param triphthong the triphthong in its clean form (ONLY the two vowels, no diacritics etc.)
     * @return the resulting feature vector
     */
    private int[] encodeTriphthong(String triphthong) {
        if (triphthong.length() != 3) {
            System.err.println("Unable to encode " + triphthong + " as triphthong!");
            return null;
        }

        String leadingDiphthong = triphthong.substring(0, 2);
        int[] leadingDiphthongFeatures = encodeDiphthong(leadingDiphthong);
        String closingDiphthong = triphthong.substring(1);
        int[] closingDiphthongFeatures = encodeDiphthong(closingDiphthong);

        if (leadingDiphthongFeatures == null || closingDiphthongFeatures == null) {
            System.err.println("Unable to encode " + triphthong + " as triphthong!");
            return null;
        }

        int[] featureVector = Arrays.copyOf(leadingDiphthongFeatures, leadingDiphthongFeatures.length);

        // all diphthong-specific features are present in the triphthong if they are present in one of the
        // underlying diphthongs.
        for (int i = 24; i < 30; i++) {
            if (closingDiphthongFeatures[i] == 1) {
                featureVector[i] = 1;
            }
        }

        // special case rounding: secondrounded should only be present if the triphthong ends in a rounded vowel.
        // feature round is used to encode for rounding of the first or second vowel.
        featureVector[30] = closingDiphthongFeatures[30];

        if (closingDiphthongFeatures[18] == 1) {
            featureVector[18] = 1;
        }

        featureTable.put(triphthong, featureVector);

        return featureVector;
    }

    /**
     * Cleans up a polyphthong to enable its encoding. Strips off tying bows and non-syllability markers and
     * relocates potential diacritics to the end, so they can be handled properly.
     * @param polyphthong the polyphthong in question
     * @return the cleaned up and reordered polyphthong
     */
    private String handlePolyphthong(String polyphthong) {
        StringBuilder polyphthongWithoutDiacriticsBuilder = new StringBuilder();
        StringBuilder diacriticsBuilder = new StringBuilder();

        for (char c : polyphthong.toCharArray()) {
            // ignore tying bows and non-syllabicity markers
            if (c == '͡' || c == '̯') {
                continue;
            }

            String character = String.valueOf(c);

            // ignore duplicate diacritics for memory efficiency
            if (diacriticsBuilder.indexOf(character) != -1) {
                continue;
            }

            if (vowelDimensions.containsKey(character)) {
                polyphthongWithoutDiacriticsBuilder.append(character);
            } else {
                diacriticsBuilder.append(character);
            }
        }

        String polyphthongWithoutDiacritics = polyphthongWithoutDiacriticsBuilder.toString();
        String diacritics = diacriticsBuilder.toString();

        if (polyphthongWithoutDiacritics.length() == 3) {
            encodeTriphthong(polyphthongWithoutDiacritics);
        } else if (polyphthongWithoutDiacritics.length() == 2) {
            encodeDiphthong(polyphthongWithoutDiacritics);
        } else {
            return null;
        }

        return polyphthongWithoutDiacritics + diacritics;
    }

    private int getVowelCount(String sound) {
        int vowelCount = 0;

        for (char c : sound.toCharArray()) {
            if (vowelDimensions.containsKey(String.valueOf(c))) {
                vowelCount++;
            }
        }

        return vowelCount;
    }

    public boolean contains(String key) {
    	return get(key) != null;
    }

    public boolean isVowel(String sound) {
        int[] features = get(sound);

        if (features == null) {
            return false;
        }

        return features[2] == -1;
    }

    public boolean isConsonant(String sound) {
        int[] features = get(sound);

        if (features == null) {
            return false;
        }

        return features[2] == 1;
    }

    public boolean isMetasymbol(String sound) {
        return metasymbols.contains(sound);
    }

    public boolean defineMetasymbol(String metasymbol, int[] features) {
        // TODO proper logging?
        // metasymbol can't be defined if string representation is already defined as symbol (IPA or metasymbol)
        // OR if feature vector is malformatted
        if (this.contains(metasymbol) || features.length != this.features.size()) return false;

        this.metasymbols.add(metasymbol);
        this.featureTable.put(metasymbol, features);

        return true;
    }

    public boolean defineMetasymbol(String metasymbol, String featureString) {
        featureString = featureString.replace(" ", "").
                replace("[", "").replace("]", "");
        String[] features = featureString.split(",");
        return defineMetasymbol(metasymbol, features);
    }

    public boolean defineMetasymbol(String metasymbol, String[] features) {
        int[] featureVector = new int[this.features.size()];

        for (String feature : features) {
            char sign = feature.charAt(0);
            String featureName = feature.substring(1);
            int featureIdx = this.features.indexOf(featureName);

            if (sign == '+') {
                featureVector[featureIdx] = 1;
            } else if (sign == '-') {
                featureVector[featureIdx] = -1;
            } else {
                return false;
            }
        }

        return defineMetasymbol(metasymbol, featureVector);
    }

    public Set<String> filterEligibleSoundsForMetasymbol(String metasymbol, Collection<String> sounds) {
        if (!this.isMetasymbol(metasymbol)) return null;

        Set<String> eligibleSounds = new HashSet<>(sounds);
        int[] definedFeatures = this.get(metasymbol);

        for (String candidateSound : sounds) {
            if (!this.contains(candidateSound)) {
                eligibleSounds.remove(candidateSound);
                continue;
            }
            int[] candidateFeatures = this.get(candidateSound);

            for (int featIdx = 0; featIdx < definedFeatures.length; featIdx++) {
                int featValue = definedFeatures[featIdx];
                if (featValue == 0) continue;

                if (candidateFeatures[featIdx] != featValue) {
                    eligibleSounds.remove(candidateSound);
                    break;
                }
            }
        }

        return eligibleSounds;
    }

    public boolean isTone(String sound) {
        int[] features = get(sound);

        if (features == null) {
            return false;
        }

        return features[2] == 0;
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

    public double[] encodeDirectedPair(String sound1, String sound2) {
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

        int[] soundPairFeatures = new int[features.size() * 2];
        System.arraycopy(sound1Features, 0, soundPairFeatures, 0, features.size());
        System.arraycopy(sound2Features, 0, soundPairFeatures, features.size(), features.size());

        // converts int array to double array
        return Arrays.stream(soundPairFeatures).asDoubleStream().toArray();
    }

    public Map<String, int[]> getFeatureTable() {
        return featureTable;
    }

    public List<String> getFeatureNames() {
        return features;
    }

    public boolean removeMetasymbol(String symbol) {
        if (metasymbols.contains(symbol)) {
            featureTable.remove(symbol);
            metasymbols.remove(symbol);
            return true;
        }

        return false;
    }

    public boolean isFeature(String featureName) {
        return features.contains(featureName);
    }
}
