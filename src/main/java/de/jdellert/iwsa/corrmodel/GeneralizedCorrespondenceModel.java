package de.jdellert.iwsa.corrmodel;

import de.jdellert.iwsa.corrmodel.neuralmodel.PmiScoreModel;
import de.jdellert.iwsa.features.IpaFeatureTable;
import de.jdellert.iwsa.sequence.GeneralizedPhoneticSymbolTable;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;
import de.tuebingen.sfs.eie.shared.util.RankingEntry;

import java.io.IOException;
import java.util.*;
import java.util.zip.DataFormatException;

/**
 * Wrap around neural PmiScoreModel, reimplements the interface of CorrespondenceModel, using score object to cache lookup results
 * Result is modifiable by calling setScore(..), this will be placed in the cache and therefore override lookup in the neural model
 */
public class GeneralizedCorrespondenceModel extends CorrespondenceModel {
    CorrespondenceModel directlyEstimatedScores;
    PmiScoreModel pairwiseSimilarityModel;
    PmiScoreModel gapModel;
    IpaFeatureTable featureTable;
    Set<String> symbolsEncountered;

    public GeneralizedCorrespondenceModel() throws DataFormatException, IOException {
        super(new GeneralizedPhoneticSymbolTable());
        pairwiseSimilarityModel = PmiScoreModel.loadPairwiseNeuralModel();
        gapModel = PmiScoreModel.loadGapModel();
        featureTable = new IpaFeatureTable();
        directlyEstimatedScores = CorrespondenceModelStorage.readGlobalModelFromFile("src/test/resources/northeuralex-0.9/global-nw-retokenized.corr");
        symbolsEncountered = directlyEstimatedScores.symbolTable.getDefinedSymbols();
    }

    public CorrespondenceModel getDirectlyEstimatedSubmodel() {
        return directlyEstimatedScores;
    }

    public List<RankingEntry<String>> mostSimilarSymbols(String symbol, Set<String> options) {
        List<RankingEntry<String>> similarSymbolRanking = new ArrayList<>();
        long symbolRep = ((long) symbolTable.toInt(symbol)) * symbolTable.getSize();
        for (String option : options) {
            double score = getScore(symbolRep + symbolTable.toInt(option));
            similarSymbolRanking.add(new RankingEntry<>(option, score));
        }
        Collections.sort(similarSymbolRanking, Comparator.reverseOrder());
        System.err.println("  most similar to " + symbol + ": " + similarSymbolRanking);
        return similarSymbolRanking;
    }

    public double getScore(int symbol1ID, int symbol2ID) {
        if (symbol1ID < 0) {
            return symbol2ID < 0 ? 1.0 : -1.0;
        } else if (symbol2ID < 0) return -1.0;
        long symbolPairID = symbolTable.getSize() * (long) symbol1ID + symbol2ID;
        return getScore(symbolPairID);
    }

    public double getScore(String symbol1, String symbol2) {
        return getScore(symbolTable.toInt(symbol1), symbolTable.toInt(symbol2));
    }

    public Double getScoreOrNull(long symbolPairID) {
        return getScore(symbolPairID);
    }

    public Double getScoreOrNull(int symbol1ID, int symbol2ID) {
        if (symbol1ID < 0) {
            return symbol2ID < 0 ? 1.0 : -1.0;
        } else if (symbol2ID < 0) return -1.0;
        long symbolPairID = symbolTable.getSize() * (long) symbol1ID + symbol2ID;
        return getScoreOrNull(symbolPairID);
    }

    public double getScore(long symbolPairId) {
        Double score = scores.get(symbolPairId);
        if (score != null) return score;
        //first attempt direct lookup in the older, more limited model
        int symbol1Id = (int) (symbolPairId / symbolTable.getSize());
        int symbol2Id = (int) (symbolPairId % symbolTable.getSize());
        String symbol1 = symbolTable.toSymbol(symbol1Id);
        String symbol2 = symbolTable.toSymbol(symbol2Id);
        score = directlyEstimatedScores.getScore(symbol1, symbol2);
        System.err.println("computing score for symbol pair " + symbolPairId + " (" + symbol1 + "," + symbol2 + ");" +
                "\tNorthEuraLex-trained model says " + ((score != null && score != 0.0) ? score : "null/0.0, falling back to neural model"));
        //the case where we need to perform lookup in the neural model
        if (score == null || score == 0.0) {
            if (symbol1.equals(symbol2)) {
                score = getSelfSimilarityScoreFromNeuralModel(symbol1);
            }
            else if (symbol1.equals("-")) {
                score = getScoreFromNeuralGapModel(symbol2);
            }
            else if (symbol2.equals("-")) {
                score = getScoreFromNeuralGapModel(symbol1);
            }
            else {
                score = getScoreFromNeuralCorrespondenceModel(symbol1, symbol2);
            }
            symbolsEncountered.add(symbol1);
            symbolsEncountered.add(symbol2);
        }
        //cache the result
        scores.put(symbolPairId, score);
        return score;
    }

    public double getScoreFromNeuralGapModel(String symbol) {
        double score = 0.0;
        int[] encodedSymbol = featureTable.get(symbol);
        if (encodedSymbol == null) {
            System.err.println("  ERROR: feature model returned null/0.0 for symbol");
        } else {
            double[] transformedEncoding = new double[encodedSymbol.length];
            for(int i = 0; i < encodedSymbol.length; i++) {
                transformedEncoding[i] = encodedSymbol[i];
            }
            score = gapModel.predict(new double[][]{transformedEncoding})[0];
            System.err.println("  gapModel predicts " + score);
        }
        return score;
    }

    /**
     * Rough estimate because current neural model performed badly;
     * for now, we only ensure that every symbol is most likely to be aligned with itself,
     * while attempting to reflect stability by estimating it from existing scores.
     * @param symbol the symbol to get the self-similarity (PMI) score for
     * @return
     */
    public double getSelfSimilarityScoreFromNeuralModel(String symbol) {
        double score = 0.0;
        Set<String> candidateSymbols = new TreeSet<>(symbolsEncountered);
        candidateSymbols.remove(symbol);
        List<RankingEntry<String>> mostSimilarSymbols = mostSimilarSymbols(symbol, candidateSymbols);
        RankingEntry<String> mostSimilar = mostSimilarSymbols.get(0);
        double mostSimilarScore = mostSimilar.value;
        double neuralSelfSimilarity = getScoreFromNeuralCorrespondenceModel(symbol, symbol);
        if (neuralSelfSimilarity > mostSimilarScore) {
            score = neuralSelfSimilarity;
            System.err.println("  symbol received highest score with itself in pairwise neural model, returning " + neuralSelfSimilarity);
        } else {
            score = mostSimilar.value + 0.5;
            if (score < 1.5) score = 1.5;
            System.err.println("  most similar symbol encountered so far was " + mostSimilar.key + " at " + mostSimilarScore + ", returning" + score);
        }
        return score;
    }

    public double getScoreFromNeuralCorrespondenceModel(String symbol1, String symbol2) {
        double score = 0.0;
        double[] encodedPair = featureTable.encodePair(symbol1, symbol2);
        if (encodedPair == null) {
            System.err.println("  ERROR: feature model returned null/0.0 for symbol pair (" + symbol1 + "," + symbol2 + ")");
        } else {
            score = pairwiseSimilarityModel.predict(new double[][]{encodedPair})[0];
            System.err.println("  pairwiseSimilarityModel predicts " + score);
        }
        return score;
    }
}
