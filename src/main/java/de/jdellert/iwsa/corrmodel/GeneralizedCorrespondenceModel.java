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
    IpaFeatureTable featureTable;

    public GeneralizedCorrespondenceModel() throws DataFormatException, IOException {
        super(new GeneralizedPhoneticSymbolTable());
        pairwiseSimilarityModel = PmiScoreModel.loadPairwiseNeuralModel();
        featureTable = new IpaFeatureTable();
        directlyEstimatedScores = CorrespondenceModelStorage.readGlobalModelFromFile("src/test/resources/northeuralex-0.9/global-nw-retokenized.corr");
    }

    public CorrespondenceModel getDirectlyEstimatedSubmodel() {
        return directlyEstimatedScores;
    }

    public List<RankingEntry<String>> mostSimilarSymbols(String symbol, Set<String> options) {
        List<RankingEntry<String>> similarSymbolRanking = new ArrayList<>();
        int symbolRep = symbolTable.toInt(symbol) * symbolTable.getSize();
        for (String option : options) {
            double score = getScore(symbolRep + symbolTable.toInt(option));
            similarSymbolRanking.add(new RankingEntry<>(option, score));
        }
        Collections.sort(similarSymbolRanking, Comparator.reverseOrder());
        System.err.println("Most similar to " + symbol + ": " + similarSymbolRanking);
        return similarSymbolRanking;
    }

    public double getScore(int symbolPairId) {
        Double score = scores.get(symbolPairId);
        if (score != null) return score;
        //first attempt direct lookup in the older, more limited model
        int symbol1Id = symbolPairId / symbolTable.getSize();
        int symbol2Id = symbolPairId % symbolTable.getSize();
        String symbol1 = symbolTable.toSymbol(symbol1Id);
        String symbol2 = symbolTable.toSymbol(symbol2Id);
        score = directlyEstimatedScores.getScore(symbol1, symbol2);
        System.err.println("computing score for symbol pair " + symbolPairId + " (" + symbol1 + "," + symbol2 + ");" +
                           " NELex says " + ((score != null || score == 0.0) ? score : "null/0.0, falling back to neural model"));
        //the case where we need to perform lookup in the neural model
        if (score == null || score == 0.0) {
            double[] encodedPair = featureTable.encodePair(symbol1, symbol2);
            if (encodedPair == null) {
                System.err.println("  ERROR: feature model returned null/0.0 for symbol pair (" + symbol1 + "," + symbol2 + "), symbolPairId = " + symbolPairId);
                score = 0.0;
            } else {
                score = pairwiseSimilarityModel.predict(new double[][]{encodedPair})[0];
                System.err.println("  pairwiseSimilarityModel predicts " + score);
            }
        }
        //cache the result
        scores.put(symbolPairId, score);
        return score;
    }
}
