package de.jdellert.iwsa.align;

import de.jdellert.iwsa.sequence.PhoneticString;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;

public class PhoneticStringAlignment {
	PhoneticString str1;
	PhoneticString str2;
	public double alignmentScore;
	public double normalizedAlignmentScore;

	public String toString(PhoneticSymbolTable symbolTable) {
		return str1.toString(symbolTable) + "\n" + str2.toString(symbolTable);
	}
}
