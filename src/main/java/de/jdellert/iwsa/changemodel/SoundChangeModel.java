package de.jdellert.iwsa.changemodel;

import de.jdellert.iwsa.features.IpaFeatureTable;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;

import java.util.*;

public class SoundChangeModel {
    private final SoundChangeLogitModel logitModel;
    private PhoneticSymbolTable symbolTable;
    private IpaFeatureTable featureTable;

    public SoundChangeModel(PhoneticSymbolTable symbolTable) {
        this(symbolTable, IpaFeatureTable.createFeatureTable());
    }

    public SoundChangeModel(PhoneticSymbolTable symbolTable, IpaFeatureTable featureTable) {
        this.symbolTable = symbolTable;
        this.featureTable = featureTable;
        this.logitModel = SoundChangeLogitModel.loadSoundChangeModel();
    }

    private double[] softmax(double[] logits) {
        if (logits == null) {
            return null;
        }

        double normalizingConstant = 0.0;

        for (double logit : logits) {
            normalizingConstant += Math.exp(logit);
        }

        double[] results = new double[logits.length];

        for (int i = 0; i < results.length; i++) {
            results[i] = Math.exp(logits[i]) / normalizingConstant;
        }

        return results;
    }

    /**
     * Returns a probability distribution over possible transitions of a sound in question.
     * @param keyId the ID of the sound in question
     * @param targetIds the IDs of the candidate sounds
     * @param forward if true, the forward distribution (key -> targets) is returned;
     *                otherwise returns the backward distribution (targets -> key)
     * @return the probability distribution
     */
    private double[] changeProbabilities(int keyId, int[] targetIds, boolean forward) {
        String key = symbolTable.toSymbol(keyId);

        if (!featureTable.contains(key)) {
            if (forward) {
                System.err.println("ERROR: Can't infer transition probabilities from [" + key + "]: " +
                        "Sound can't be encoded by the feature table!");
            } else {
                System.err.println("ERROR: Can't infer transition probabilities to [" + key + "]: " +
                        "Sound can't be encoded by the feature table!");
            }

            return null;
        }

        List<double[]> encodedPairs = new ArrayList<>();
        List<Integer> illegalIndices = new ArrayList<>();

        for (int i = 0; i < targetIds.length; i++) {
            int id = targetIds[i];
            String target = symbolTable.toSymbol(id);
            if (featureTable.contains(target)) {
                if (forward) {
                    encodedPairs.add(featureTable.encodeDirectedPair(key, target));
                } else {
                    encodedPairs.add(featureTable.encodeDirectedPair(target, key));
                }
            } else {
                if (forward) {
                    System.err.println("WARNING: Can't infer transition probabilities from [" + key + "]" +
                            " to [" + target + "]: Output sound can't be encoded by the feature table," +
                            " probability will be set to 0.");
                } else {
                    System.err.println("WARNING: Can't infer transition probabilities from [" + target + "]" +
                            " to [" + key + "]: Input sound can't be encoded by the feature table," +
                            " probability will be set to 0.");
                }
                // add placeholder for non-encodable sounds
                encodedPairs.add(featureTable.encodeDirectedPair("-", "-"));
                illegalIndices.add(i);
            }
        }

        double[][] inputs = encodedPairs.toArray(new double[encodedPairs.size()][]);
        double[] logits = logitModel.predict(inputs);

        // assign 0 probability to sounds that can not be encoded
        for (int idx : illegalIndices) {
            logits[idx] = -99999;
        }

        return softmax(logits);
    }

    public Map<String,Double> changeProbabilities(String key, Collection<String> targets, boolean forward) {
        List<String> orderedTargets = new ArrayList<>(targets);
        int[] targetIds = new int[targets.size()];

        for (int i = 0; i < targets.size(); i++) {
            targetIds[i] = symbolTable.toInt(orderedTargets.get(i));
        }

        int keyId = symbolTable.toInt(key);
        double[] probabilityDistribution = changeProbabilities(keyId, targetIds, forward);

        if (probabilityDistribution == null) {
            return null;
        }

        Map<String, Double> probabilityDistributionMap = new HashMap<>();

        for (int i = 0; i < orderedTargets.size(); i++) {
            String target = orderedTargets.get(i);
            probabilityDistributionMap.put(target, probabilityDistribution[i]);
        }

        return probabilityDistributionMap;
    }

    public double[] changeProbabilitiesFromSound(int inputSoundId, int[] outputSoundIds) {
        return changeProbabilities(inputSoundId, outputSoundIds, true);
    }

    Map<String,Double> changeProbabilitiesFromSound(String inputSound, Collection<String> outputSounds) {
        return changeProbabilities(inputSound, outputSounds, true);
    }

    public double[] changeProbabilitiesIntoSound(int outputSoundId, int[] inputSoundIds) {
        return changeProbabilities(outputSoundId, inputSoundIds, false);
    }

    Map<String,Double> changeProbabilitiesIntoSound(String outputSound, Collection<String> inputSounds) {
        return changeProbabilities(outputSound, inputSounds, false);
    }

    public static void main(String[] args) {
        PhoneticSymbolTable symbolTable = new PhoneticSymbolTable();
        String[] inventory = "OHA iː yː uː ɪ eː ʏ øː ə ʊ oː ɛ ɛː œ ɐ ɔ aː a ʊɪ̯ HEHE ɔʏ̯ aɪ̯ aʊ̯ m n ŋ p t k ʔ b d g pf ts tʃ dʒ f s ʃ ç x h v z ʒ j l r - AAAHHH".split(" ");
        for (String symbol : inventory) {
            symbolTable.defineSymbol(symbol);
        }

        SoundChangeModel model = new SoundChangeModel(symbolTable);

        double[] probabilitesFromP = model.changeProbabilitiesFromSound(symbolTable.toInt("p"), symbolTable.encode(inventory));
        double[] probabilitesIntoP = model.changeProbabilitiesIntoSound(symbolTable.toInt("p"), symbolTable.encode(inventory));

        for (int i = 0; i < probabilitesFromP.length; i++) {
            System.out.println("[p] -> [" + inventory[i] + "]: " + probabilitesFromP[i]);
            System.out.println("[" + inventory[i] + "] -> [p]: " + probabilitesIntoP[i]);
            System.out.println("----------------------------------------------------------------------------------");
        }

        List<String> inventoryAsList = new ArrayList<>(Arrays.asList(inventory));
        Map<String, Double> probabilitiesMapFromP = model.changeProbabilitiesFromSound("p", inventoryAsList);
        Map<String, Double> probabilitiesMapIntoP = model.changeProbabilitiesIntoSound("p", inventoryAsList);

        for (Map.Entry<String, Double> entry : probabilitiesMapFromP.entrySet()) {
            System.out.println("[p] -> [" + entry.getKey() + "]: " + entry.getValue());
        }

        System.out.println("----------------------------------------------------------------------------------");

        for (Map.Entry<String, Double> entry : probabilitiesMapIntoP.entrySet()) {
            System.out.println("[" + entry.getKey() + "] -> [p]: " + entry.getValue());
        }
    }
}
