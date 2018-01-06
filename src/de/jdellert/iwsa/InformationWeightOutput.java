package de.jdellert.iwsa;

import java.io.IOException;

import de.jdellert.iwsa.data.CLDFImport;
import de.jdellert.iwsa.data.LexicalDatabase;
import de.jdellert.iwsa.infomodel.InformationModel;
import de.jdellert.iwsa.infomodel.InformationModelInference;
import de.jdellert.iwsa.sequence.PhoneticString;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;
import de.jdellert.iwsa.util.io.Formatting;

/**
 * A class for inspecting language-specific information weightings. The output
 * gives the information content values of each segment of all strings for some language.
 * 
 * @author jdellert
 *
 */

public class InformationWeightOutput {
	public static void main(String[] args) {
		try {
			LexicalDatabase database = CLDFImport.loadDatabase(args[0], true);
			PhoneticSymbolTable symbolTable = database.getSymbolTable();

			String langCode = args[1];
			int langID = database.getIDForLanguageCode(langCode);
			if (langID == -1) {
				System.err.println("ERROR: language code " + langCode + " not represented in database!");
				System.exit(1);
			}

			InformationModel model = InformationModelInference.inferInformationModelForLanguage(langID, database,
					symbolTable);
			
			for (int formID : database.getFormIDsForLanguage(langID)) {
				PhoneticString form = database.getForm(formID);
				int[] s = form.segments;
				StringBuilder formString = new StringBuilder();
				StringBuilder infoString = new StringBuilder();
				for (int i = 0; i < s.length; i++)
				{
					formString.append("  " + symbolTable.toSymbol(s[i]) + "   ");
					infoString.append(Formatting.str3f(model.informationContent(s, i)) + " ");
				}
				formString.deleteCharAt(formString.length() - 1);
				infoString.deleteCharAt(infoString.length() - 1);
				System.out.println(formString);
				System.out.println(infoString);
				System.out.println();
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
