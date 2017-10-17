package de.jdellert.iwsa;

import java.io.IOException;
import java.util.Arrays;

import de.jdellert.iwsa.align.LevenshteinAlignmentAlgorithm;
import de.jdellert.iwsa.align.PhoneticStringAlignment;
import de.jdellert.iwsa.data.CLDFImport;
import de.jdellert.iwsa.data.LexicalDatabase;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;

public class ConceptLevelEditDistanceOutput {

	public static void main(String[] args) {
		try {
			LexicalDatabase database = CLDFImport.loadDatabase(args[0], true);
			PhoneticSymbolTable symbolTable = database.getSymbolTable();
			for (int hinFormID : database.lookupFormIDs("hin", "Haus::N")) {
				for (int benFormID : database.lookupFormIDs("ben", "Haus::N")) {
					PhoneticStringAlignment alignment = LevenshteinAlignmentAlgorithm.constructAlignment(database.getForm(hinFormID), database.getForm(benFormID));
					System.err.println(alignment.alignmentScore + "\t" + alignment.normalizedAlignmentScore);
					System.err.println(alignment.toString(symbolTable));
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
