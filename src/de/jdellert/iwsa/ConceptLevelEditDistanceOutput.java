package de.jdellert.iwsa;

import java.io.IOException;
import java.util.Arrays;

import de.jdellert.iwsa.data.CLDFImport;
import de.jdellert.iwsa.data.LexicalDatabase;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;

public class ConceptLevelEditDistanceOutput {

	public static void main(String[] args) {
		try {
			LexicalDatabase database = CLDFImport.loadDatabase(args[0]);
			PhoneticSymbolTable symbolTable = database.getSymbolTable();
			for (int formID : database.lookupFormIDs("fin", "Haus::N")) {
				System.out.println(Arrays.toString(symbolTable.decode(database.getForm(formID).segments)));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
