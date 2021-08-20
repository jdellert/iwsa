package de.jdellert.iwsa.corrmodel;

import de.jdellert.iwsa.sequence.PhoneticString;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.InputMismatchException;

public class ConfidenceScore {
    private PhoneticSymbolTable symbolTable;
    private int[] counts;
    private int sumCounts;

    public ConfidenceScore(PhoneticSymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.counts = new int[symbolTable.getSize()];
        this.sumCounts = 0;
    }

    public void addAlignment(String[] str1Segments, String[] str2Segments) {
        for (int symbolIndex : symbolTable.encode(str1Segments)) {
            counts[symbolIndex]++;
            sumCounts++;
        }
        for (int symbolIndex : symbolTable.encode(str2Segments)) {
            counts[symbolIndex]++;
            sumCounts++;
        }
    }

    public double getConfidenceScoreForPair(String symbol1, String symbol2) {
        return getConfidenceScoreForPair(symbolTable.toInt(symbol1), symbolTable.toInt(symbol2));
    }

    public double getConfidenceScoreForPair(int symbol1Idx, int symbol2Idx) {
        int count1 = counts[symbol1Idx];
        int count2 = counts[symbol2Idx];
        double logFreq1 = Math.log((double) count1 / sumCounts);
        double logFreq2 = Math.log((double) count2 / sumCounts);
        return (logFreq1 + logFreq2) / 2;
    }

    public PhoneticSymbolTable getSymbolTable() {
        return symbolTable;
    }

    public int[] getCounts() {
        return counts;
    }

    public int getSumCounts() {
        return sumCounts;
    }

    public void concatenate(ConfidenceScore otherConfidenceScore) {
        if (symbolTable.getSize() != otherConfidenceScore.getSymbolTable().getSize()) {
            throw new InputMismatchException("Confidence Score Objects must share the same Phonetic Symbol Table!");
        }

        for (int i = 0; i < counts.length; i++) {
            counts[i] += otherConfidenceScore.getCounts()[i];
        }

        sumCounts += otherConfidenceScore.getSumCounts();
    }

    public void toFile(String filePath) throws IOException {
        FileWriter writer = new FileWriter(new File(filePath));
        for (int i = 0; i < counts.length; i++) {
            String symbol1 = symbolTable.toSymbol(i);
            for (int j = (i+1); j < counts.length; j++) {
                String symbol2 = symbolTable.toSymbol(j);
                double confidenceScore = getConfidenceScoreForPair(i, j);
                writer.write(symbol1 + "\t" + symbol2 + "\t" + confidenceScore + "\n");
            }
        }
        writer.close();
    }
}
