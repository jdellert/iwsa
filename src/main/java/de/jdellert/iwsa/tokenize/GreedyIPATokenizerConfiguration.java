package de.jdellert.iwsa.tokenize;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Allows to specify a set of multi-character transformations
 * for greedy IPA tokenization with accompanying normalizations.
 * <p>
 * Serves as an alternative option for configuring an IPATokenizer.
 */
public class GreedyIPATokenizerConfiguration extends IPATokenizerConfiguration {
    //define a number of characters which are deleted during the first pass (cleanup stage)
    Set<Character> ignoredSymbols;

    //structure for greedy lookahead in second pass (the actual transformation)
    Map<String, List<String>> sequenceToSymbols;
    int lookahead;

    public GreedyIPATokenizerConfiguration() {
        super();
        ignoredSymbols = new TreeSet<Character>();
        sequenceToSymbols = new TreeMap<String, List<String>>();
        lookahead = 1;
    }

    private void addSequence(String sequence, List<String> symbols) {
        sequenceToSymbols.put(sequence, symbols);
        if (sequence.length() > lookahead) {
            lookahead = sequence.length();
        }
    }

    public void addUnchangedSymbol(String symbol) {
        addSequence(symbol, Arrays.asList(new String[]{symbol}));
    }

    public void addSequenceTransformation(String sequence, List<String> symbols) {
        addSequence(sequence, symbols);
    }

    public void addIgnoredSymbol(Character symbol) {
        ignoredSymbols.add(symbol);
    }

    public void factorInGeminationSymbol(String geminationSymbol) {
        List<String> existingSymbols = new LinkedList<>(sequenceToSymbols.keySet());
        for (String existingSymbol : existingSymbols) {
            String symbolWithGemination = existingSymbol + geminationSymbol;
            List<String> transformation = new LinkedList<>(sequenceToSymbols.get(existingSymbol));
            transformation.addAll(sequenceToSymbols.get(existingSymbol));
            addSequence(symbolWithGemination, transformation);
        }
    }

}
