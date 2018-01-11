package de.jdellert.iwsa.align;

import de.jdellert.iwsa.corrmodel.CorrespondenceModel;
import de.jdellert.iwsa.sequence.PhoneticString;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;

public class PhoneticStringAlignment {
	PhoneticString str1;
	PhoneticString str2;
	public double alignmentScore;
	public double normalizedDistanceScore;
	
	public String toString(PhoneticSymbolTable symbolTable) {
		return str1.toString(symbolTable) + "\n" + str2.toString(symbolTable);
	}

	public int getLength() {
		return str1.segments.length;
	}

	public int getSymbolPairIDAtPos(int pos, PhoneticSymbolTable symbolTable) {
		return str1.segments[pos] * symbolTable.getSize() + str2.segments[pos];
	}

}
