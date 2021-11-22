package de.jdellert.iwsa.corrmodel;

import java.util.Map;
import java.util.TreeMap;

import de.jdellert.iwsa.sequence.PhoneticSymbolTable;

public class CorrespondenceModel {
	PhoneticSymbolTable symbolTable;
	// symbol pair ID => PMI score
	Map<Long, Double> scores;
	String dbPath;

	public CorrespondenceModel(PhoneticSymbolTable symbolTable) {
		this.symbolTable = symbolTable;
		this.scores = new TreeMap<Long, Double>();
	}

	public PhoneticSymbolTable getSymbolTable() {
		return symbolTable;
	}

	public void setScore(long symbolPairID, double score) {
		scores.put(symbolPairID, score);
	}

	public void setScore(int symbol1ID, int symbol2ID, double score) {
		long symbolPairID = symbolTable.getSize() * (long) symbol1ID + symbol2ID;
		setScore(symbolPairID, score);
	}

	public double getScore(long symbolPairID) {
		Double score = scores.get(symbolPairID);
		if (score == null)
			score = 0.0;
		return score;
	}

	public double getScore(int symbol1ID, int symbol2ID) {
		long symbolPairID = symbolTable.getSize() * (long) symbol1ID + symbol2ID;
		return getScore(symbolPairID);
	}

	public double getScore(String symbol1, String symbol2) {
		return getScore(symbolTable.toInt(symbol1), symbolTable.toInt(symbol2));
	}

	public Double getScoreOrNull(long symbolPairID) {
		return scores.get(symbolPairID);
	}

	public Double getScoreOrNull(int symbol1ID, int symbol2ID) {
		long symbolPairID = symbolTable.getSize() * (long) symbol1ID + symbol2ID;
		return getScoreOrNull(symbolPairID);
	}
	
	

	public String getDbPath() {
		return dbPath;
	}

	public void setDbPath(String dbPath) {
		this.dbPath = dbPath;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < symbolTable.getSize(); i++)
			sb.append("\t").append(symbolTable.toSymbol(i));
		sb.append("\n");

		for (int i = 0; i < symbolTable.getSize(); i++) {
			sb.append(symbolTable.toSymbol(i));
			for (int j = 0; j < symbolTable.getSize(); j++) {
				sb.append("\t").append(getScore(i, j));
			}
			sb.append("\n");
		}

		return sb.toString();
	}
}
