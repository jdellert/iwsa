package de.jdellert.iwsa.infomodel;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.TreeMap;

import de.jdellert.iwsa.sequence.PhoneticSymbolTable;
import de.jdellert.iwsa.util.io.Formatting;

public class InformationModel {
	public static boolean VERBOSE = false;
	
	// gappy bigrams are also stored in this structure
	Map<Integer, Integer> observationCounts;
	// only 1/4 of the some of observation counts due to representation of gappy
	// bigrams
	double observationCountsSum = 0.0;

	double smoothingMassRatio = 0.2;

	int numSymbols;
	int numTrigrams;
	int numGappyBigrams;
	PhoneticSymbolTable symbolTable;

	public InformationModel(PhoneticSymbolTable symbolTable) {
		this.symbolTable = symbolTable;
		this.numSymbols = symbolTable.getSize();
		this.numTrigrams = (numSymbols - 1) * (numSymbols - 1) * (numSymbols - 1);
		this.numGappyBigrams = (numSymbols - 1) * (numSymbols - 1);

		this.observationCounts = new TreeMap<Integer, Integer>();
	}

	public void setSmoothingMassRatio(double ratio) {
		this.smoothingMassRatio = ratio;
	}

	public void addTrigramObservation(int a, int b, int c) {
		storeObservation(trigramID(a, b, c)); // ABC
		storeGappyObservation(trigramID(a, b, 1)); // AB_
		storeGappyObservation(trigramID(a, 1, c)); // A_C
		storeGappyObservation(trigramID(1, b, c)); // _BC
	}

	public int trigramCount(int a, int b, int c) {
		Integer observationCount = observationCounts.get(trigramID(a, b, c));
		if (observationCount == null)
			observationCount = 0;
		return observationCount;
	}

	public double smoothedTrigramCount(int a, int b, int c) {
		return trigramCount(a, b, c) + ((smoothingMassRatio * observationCountsSum) / numTrigrams);
	}

	public double smoothedTrigramCount(int a, int b, int c, double bigramCount) {
		return trigramCount(a, b, c) + ((smoothingMassRatio * bigramCount) / (numSymbols - 1));
	}

	public double smoothedGappyBigramCount(int a, int b, int c) {
		return (1 + smoothingMassRatio) * trigramCount(a, b, c) + ((smoothingMassRatio * observationCountsSum) / numGappyBigrams);
	}

	public double informationContent(int[] s, int i) {
		int a = (i > 1 ? s[i - 2] : 0);
		int b = (i > 0 ? s[i - 1] : 0);
		int c = s[i];
		int d = (i < s.length - 1 ? s[i + 1] : 0);
		int e = (i < s.length - 2 ? s[i + 2] : 0);
		return informationContent(a, b, c, d, e);
	}

	public double informationContent(int a, int b, int c, int d, int e) {
		if (VERBOSE) System.out.print("c(" + Formatting.intRPad(a, 3) + "," + Formatting.intRPad(b, 3) + ","
				+ Formatting.intRPad(c, 3) + "," + Formatting.intRPad(d, 3) + "," + Formatting.intRPad(e, 3) + "):  ");
		double abCount = smoothedGappyBigramCount(a, b, 1);
		double bdCount = smoothedGappyBigramCount(b, 1, d);
		double deCount = smoothedGappyBigramCount(1, d, e);
		double abcCount = smoothedTrigramCount(a, b, c);
		double bcdCount = smoothedTrigramCount(b, c, d);
		double cdeCount = smoothedTrigramCount(c, d, e);

		double obsProb = (abcCount + bcdCount + cdeCount)/(abCount + bdCount + deCount);
		
		if (VERBOSE) System.out.print(Formatting.str3f(obsProb) + " = ");
		if (VERBOSE) System.out.print("(" + Math.round(abcCount) + "+" + Math.round(bcdCount) + "+" + Math.round(cdeCount) +")/"); 
		if (VERBOSE) System.out.println("(" + Math.round(abCount) + "+" + Math.round(bdCount) + "+" + Math.round(deCount) +")");
		
		return -Math.log(obsProb);
	}

	private void storeObservation(int trigramID) {
		Integer count = observationCounts.get(trigramID);
		if (count == null) {
			count = 0;
		}
		observationCounts.put(trigramID, count + 1);
		observationCountsSum += 1.0;
	}

	private void storeGappyObservation(int trigramID) {
		Integer count = observationCounts.get(trigramID);
		if (count == null) {
			count = 0;
		}
		observationCounts.put(trigramID, count + 1);
	}

	public int trigramID(int symbolA, int symbolB, int symbolC) {
		return symbolA * numSymbols * numSymbols + symbolB * numSymbols + symbolC;
	}

	public void printCounts(OutputStream output) throws IOException {
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(output));
		for (int a = 0; a < symbolTable.getSize(); a++) {
			if (a == 1)
				continue;
			for (int b = 0; b < symbolTable.getSize(); b++) {
				if (b == 1)
					continue;
				int abCount = trigramCount(a, b, 1);
				if (abCount > 0) {
					String abString = symbolTable.toSymbol(a) + symbolTable.toSymbol(b);
					out.write(abString + "-" + ":" + abCount);
					for (int c = 0; c < symbolTable.getSize(); c++) {
						if (c == 1)
							continue;
						int abcCount = trigramCount(a, b, c);
						if (abcCount > 0) {
							out.write(" " + abString + symbolTable.toSymbol(c) + ":" + abcCount);
						}
					}
					out.write("\n");
				}
			}
		}
		for (int a = 0; a < symbolTable.getSize(); a++) {
			if (a == 1)
				continue;
			for (int c = 0; c < symbolTable.getSize(); c++) {
				if (c == 1)
					continue;
				int acCount = trigramCount(a, 1, c);
				if (acCount > 0) {
					String aString = symbolTable.toSymbol(a);
					String cString = symbolTable.toSymbol(c);
					out.write(aString + "-" + cString + ":" + acCount);
					for (int b = 0; b < symbolTable.getSize(); b++) {
						if (b == 1)
							continue;
						int abcCount = trigramCount(a, b, c);
						if (abcCount > 0) {
							out.write(" " + aString + symbolTable.toSymbol(b) + cString + ":" + abcCount);
						}
					}
					out.write("\n");
				}
			}
		}
		for (int b = 0; b < symbolTable.getSize(); b++) {
			if (b == 1)
				continue;
			for (int c = 0; c < symbolTable.getSize(); c++) {
				if (c == 1)
					continue;
				int bcCount = trigramCount(1, b, c);
				if (bcCount > 0) {
					String bcString = symbolTable.toSymbol(b) + symbolTable.toSymbol(c);
					out.write("-" + bcString + ":" + bcCount);
					for (int a = 0; a < symbolTable.getSize(); a++) {
						if (a == 1)
							continue;
						int abcCount = trigramCount(a, b, c);
						if (abcCount > 0) {
							out.write(" " + symbolTable.toSymbol(a) + bcString + ":" + abcCount);
						}
					}
					out.write("\n");
				}
			}
		}
		out.close();
	}
}
