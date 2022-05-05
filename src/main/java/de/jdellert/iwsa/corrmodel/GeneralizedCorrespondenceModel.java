package de.jdellert.iwsa.corrmodel;

import de.jdellert.iwsa.corrmodel.neuralmodel.PmiScoreModel;
import de.jdellert.iwsa.features.IpaFeatureTable;
import de.jdellert.iwsa.sequence.GeneralizedPhoneticSymbolTable;
import de.jdellert.iwsa.util.ranking.RankingEntry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.DataFormatException;

/**
 * Wrap around neural PmiScoreModel, reimplements the interface of CorrespondenceModel, using score object to cache lookup results
 * Result is modifiable by calling setScore(..), this will be placed in the cache and therefore override lookup in the neural model
 */
public class GeneralizedCorrespondenceModel extends CorrespondenceModel {
    public static boolean VERBOSE = true;

    CorrespondenceModel directlyEstimatedScores;
    PmiScoreModel pairwiseSimilarityModel;
    PmiScoreModel gapModel;
    IpaFeatureTable featureTable;
    Set<String> symbolsEncountered;

    GeneralizedPhoneticSymbolTable generalizedSymbolTable;

    public GeneralizedCorrespondenceModel() throws DataFormatException, IOException {
        super(new GeneralizedPhoneticSymbolTable());
        generalizedSymbolTable = (GeneralizedPhoneticSymbolTable) getSymbolTable();
        pairwiseSimilarityModel = PmiScoreModel.loadPairwiseNeuralModel();
        gapModel = PmiScoreModel.loadGapModel();
        featureTable = new IpaFeatureTable();
        directlyEstimatedScores = CorrespondenceModelStorage.readGlobalModelFromFile("/de/jdellert/iwsa/neuralmodel/corrmodel/global-nw-retokenized.corr");
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
        if (VERBOSE) System.err.println("  most similar to " + symbol + ": " + similarSymbolRanking);
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
        if (VERBOSE) System.err.println("Looking up score for symbol pair (" + symbol1 + "," + symbol2 + ")");
        int symbol1ID = symbolTable.toInt(symbol1);
        int symbol2ID = symbolTable.toInt(symbol2);
        if (symbol1ID == -1) {
            if (VERBOSE) System.err.println("  WARNING: symbolTable returned -1 for \"" + symbol1 + "\", indicating that symbol is undefined");
        }
        if (symbol2ID == -1) {
            if (VERBOSE) System.err.println("  WARNING: symbolTable returned -1 for \"" + symbol2 + "\", indicating that symbol is undefined");
        }
        return getScore(symbol1ID, symbol2ID);
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
        int symbol1Id = (int) (symbolPairId / symbolTable.getSize());
        int symbol2Id = (int) (symbolPairId % symbolTable.getSize());
        String symbol1 = symbolTable.toSymbol(symbol1Id);
        String symbol2 = symbolTable.toSymbol(symbol2Id);
        //metasymbols and combined symbols need to be interpreted via the overage of their extensions
        //TODO: coarticulations should count only half, so there should be some way of declaring "half-symbols"
        //TODO: metasymbols actually need to be interpreted via sets of feature vectors (in order to unify definitions)
        List<Integer> combinedSymbolComponents1 = generalizedSymbolTable.getCombinedSymbolComponentsOrNull(symbol1Id);
        List<Integer> combinedSymbolComponents2 = generalizedSymbolTable.getCombinedSymbolComponentsOrNull(symbol2Id);
        Set<Integer> metasymbolExtensions1 = generalizedSymbolTable.getMetasymbolExtensionsOrNull(symbol1Id);
        Set<Integer> metasymbolExtensions2 = generalizedSymbolTable.getMetasymbolExtensionsOrNull(symbol2Id);
        if (combinedSymbolComponents1 != null || combinedSymbolComponents2 != null || metasymbolExtensions1 != null || metasymbolExtensions2 != null) {
            if (VERBOSE && combinedSymbolComponents1 != null) System.err.println("      " + symbol1 + " is a combined symbol, computing the average score");
            if (VERBOSE && combinedSymbolComponents2 != null) System.err.println("      " + symbol2 + " is a combined symbol, computing the average score");
            if (VERBOSE && metasymbolExtensions1 != null) System.err.println("      " + symbol1 + " is a metasymbol, computing the average score");
            if (VERBOSE && metasymbolExtensions2 != null) System.err.println("      " + symbol2 + " is a metasymbol, computing the average score");
            Set<Integer> symbols1 = new HashSet<>();
            symbols1.add(symbol1Id);
            Set<Integer> symbols2 = new HashSet<>();
            symbols2.add(symbol2Id);
            if (combinedSymbolComponents1 != null) symbols1 = new HashSet<>(combinedSymbolComponents1);
            if (combinedSymbolComponents2 != null) symbols2 = new HashSet<>(combinedSymbolComponents2);
            if (metasymbolExtensions1 != null) symbols1 = metasymbolExtensions1;
            if (metasymbolExtensions2 != null) symbols2 = metasymbolExtensions2;
            score = getAverageScore(symbols1, symbols2);
            if (VERBOSE) System.err.println("      averaged score for symbol pair " +  + symbolPairId + " (" + symbol1 + "," + symbol2 + ") was " + score);
            //cache the result
            scores.put(symbolPairId, score);
            return score;
        }

        //base case (no combination symbol and no metasymbol involved)
        //first attempt: direct lookup in the older, more limited model
        score = directlyEstimatedScores.getScore(symbol1, symbol2);
        if (VERBOSE) System.err.println("    computing score for symbol pair " + symbolPairId + " (" + symbol1 + "," + symbol2 + ");" +
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

    public double getAverageScore(Collection<Integer> symbols1, Collection<Integer> symbols2) {
        double sum = 0.0;
        for (int id1 : symbols1) {
            for (int id2 : symbols2) {
                double partialScore = getScore(id1, id2);
                if (VERBOSE) {
                    String symbol1 = generalizedSymbolTable.toSymbol(id1);
                    String symbol2 = generalizedSymbolTable.toSymbol(id2);
                    System.err.println("      partial score for (" + symbol1 + "," + symbol2 + "): " + partialScore);
                }
                sum += partialScore;
            }
        }
        return sum/(symbols1.size() * symbols2.size());
    }

    public double getScoreFromNeuralGapModel(String symbol) {
        double score = 0.0;
        int[] encodedSymbol = featureTable.get(symbol);
        if (encodedSymbol == null) {
            System.err.println("  ERROR: feature model returned null/0.0 for symbol \"" + symbol + "\"");
        } else {
            double[] transformedEncoding = new double[encodedSymbol.length];
            for(int i = 0; i < encodedSymbol.length; i++) {
                transformedEncoding[i] = encodedSymbol[i];
            }
            score = gapModel.predict(new double[][]{transformedEncoding})[0];
            if (VERBOSE) System.err.println("    gapModel predicts " + score);
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
            if (VERBOSE) System.err.println("    symbol received highest score with itself in pairwise neural model, returning " + neuralSelfSimilarity);
        } else {
            score = mostSimilar.value + 0.5;
            if (score < 1.5) score = 1.5;
            if (VERBOSE) System.err.println("    most similar symbol encountered so far was " + mostSimilar.key + " at " + mostSimilarScore + ", returning" + score);
        }
        return score;
    }

    public double getScoreFromNeuralCorrespondenceModel(String symbol1, String symbol2) {
        double score = 0.0;
        double[] encodedPair = featureTable.encodePair(symbol1, symbol2);
        if (encodedPair == null) {
            if (VERBOSE) System.err.println("    ERROR: feature model returned null/0.0 for symbol pair (" + symbol1 + "," + symbol2 + ")");
        } else {
            score = pairwiseSimilarityModel.predict(new double[][]{encodedPair})[0];
            if (VERBOSE) System.err.println("    pairwiseSimilarityModel predicts " + score);
        }
        return score;
    }
    
    public IpaFeatureTable getFeatureTable() {
    	return featureTable;
    }
}
