package de.jdellert.iwsa;

import java.io.IOException;

import de.jdellert.iwsa.data.CLDFImport;
import de.jdellert.iwsa.data.LexicalDatabase;
import de.jdellert.iwsa.infomodel.InformationModel;
import de.jdellert.iwsa.infomodel.InformationModelInference;
import de.jdellert.iwsa.sequence.PhoneticString;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;

public class ConceptLevelInformationWeightedEditDistanceOutput {
	public static void main(String[] args) {
		try {
			LexicalDatabase database = CLDFImport.loadDatabase(args[0], true);
			PhoneticSymbolTable symbolTable = database.getSymbolTable();
			
			InformationModel[] infoModels = InformationModelInference.inferInformationModels(database, symbolTable);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
