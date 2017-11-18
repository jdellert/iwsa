package de.jdellert.iwsa;

import java.io.IOException;

import de.jdellert.iwsa.data.CLDFImport;
import de.jdellert.iwsa.data.LexicalDatabase;
import de.jdellert.iwsa.infomodel.InformationModel;
import de.jdellert.iwsa.infomodel.InformationModelInference;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;

/**
 * A class for inspecting language-specific information models.
 * The output visualizes the probability of realizations for each gappy bigram encountered in the data.
 * 
 * @author jdellert
 *
 */

public class InformationModelOutput {
	
	//TODO: change this to work on stored information model files
	public static void main(String[] args)
	{
		try {
			LexicalDatabase database = CLDFImport.loadDatabase(args[0], true);
			PhoneticSymbolTable symbolTable = database.getSymbolTable();
			
			String langCode = args[1];
			int langID = database.getIDForLanguageCode(langCode);
			if (langID == -1)
			{
				System.err.println("ERROR: language code " + langCode + " not represented in database!");
				System.exit(1);
			}
			
			InformationModel model = InformationModelInference.inferInformationModelForLanguage(langID, database, symbolTable);
			model.printCounts(System.out);
			
		} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	}
}
