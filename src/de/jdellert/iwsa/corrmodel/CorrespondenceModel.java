package de.jdellert.iwsa.corrmodel;

import java.util.Map;
import java.util.TreeMap;

import de.jdellert.iwsa.sequence.PhoneticSymbolTable;

public class CorrespondenceModel {
	PhoneticSymbolTable symbolTable;
	// symbol pair ID => PMI score
	Map<Integer, Double> scores;

	public CorrespondenceModel(PhoneticSymbolTable symbolTable) {
		this.symbolTable = symbolTable;
		this.scores = new TreeMap<Integer, Double>();
	}

	public PhoneticSymbolTable getSymbolTable() {
		return symbolTable;
	}

	public void setScore(int symbolPairID, double score) {
		scores.put(symbolPairID, score);
	}

	public void setScore(int symbol1ID, int symbol2ID, double score) {
		int symbolPairID = symbolTable.getSize() * symbol1ID + symbol2ID;
		setScore(symbolPairID, score);
	}

	public double getScore(int symbolPairID) {
		Double score = scores.get(symbolPairID);
		if (score == null)
			score = 0.0;
		return score;
	}

	public double getScore(int symbol1ID, int symbol2ID) {
		int symbolPairID = symbolTable.getSize() * symbol1ID + symbol2ID;
		return getScore(symbolPairID);
	}
}
