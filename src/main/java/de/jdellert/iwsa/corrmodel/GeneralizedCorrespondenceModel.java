package de.jdellert.iwsa.corrmodel;

import de.jdellert.iwsa.corrmodel.neuralmodel.PmiScoreModel;
import de.jdellert.iwsa.features.IpaFeatureTable;
import de.jdellert.iwsa.sequence.GeneralizedPhoneticSymbolTable;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;

import java.io.IOException;
import java.util.zip.DataFormatException;

/**
 * Wrap around neural PmiScoreModel, reimplements the interface of CorrespondenceModel, using score object to cache lookup results
 * Result is modifiable by calling setScore(..), this will be placed in the cache and therefore override lookup in the neural model
 */
public class GeneralizedCorrespondenceModel extends CorrespondenceModel {
    PmiScoreModel pairwiseSimilarityModel;
    IpaFeatureTable featureTable;

    public GeneralizedCorrespondenceModel() throws DataFormatException, IOException {
        super(new GeneralizedPhoneticSymbolTable());
        pairwiseSimilarityModel = PmiScoreModel.loadPairwiseNeuralModel();
        featureTable = new IpaFeatureTable();
    }

    public double getScore(int symbolPairId) {
        Double score = scores.get(symbolPairId);
        if (score != null) return score;
        //the case where we need to perform lookup in the neural model
        int symbol1Id = symbolPairId / symbolTable.getSize();
        int symbol2Id = symbolPairId % symbolTable.getSize();
        String symbol1 = getSymbolTable().toSymbol(symbol1Id);
        String symbol2 = getSymbolTable().toSymbol(symbol2Id);
        double[] encodedPair = featureTable.encodePair(symbol1, symbol2);
        if (encodedPair == null) {
            System.err.println("ERROR: feature model returned null for symbol pair (" + symbol1 + "," + symbol2 + "), symbolPairId = " + symbolPairId);
            return 0.0;
        }
        score = pairwiseSimilarityModel.predict(new double[][] {encodedPair})[0];
        //cache the result
        scores.put(symbolPairId, score);
        return score;
    }
}
