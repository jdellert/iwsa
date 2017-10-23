package de.jdellert.iwsa;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import de.jdellert.iwsa.align.LevenshteinAlignmentAlgorithm;
import de.jdellert.iwsa.align.PhoneticStringAlignment;
import de.jdellert.iwsa.data.CLDFImport;
import de.jdellert.iwsa.data.LexicalDatabase;
import de.jdellert.iwsa.sequence.PhoneticString;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;

public class ConceptLevelEditDistanceOutput {

	public static void main(String[] args) {
		try {
			LexicalDatabase database = CLDFImport.loadDatabase(args[0], false);
			PhoneticSymbolTable symbolTable = database.getSymbolTable();
			for (int conceptID = 0; conceptID < database.getNumConcepts(); conceptID++)
			{
				List<List<Integer>> formsPerLang = database.getFormIDsForConceptPerLanguage(conceptID);
				for (int lang1ID = 0; lang1ID < database.getNumLanguages(); lang1ID++)
				{
					for (int lang2ID = lang1ID + 1; lang2ID < database.getNumLanguages(); lang2ID++)
					{
						for (int lang1FormID : formsPerLang.get(lang1ID))
						{
							PhoneticString lang1Form = database.getForm(lang1FormID);
							for (int lang2FormID : formsPerLang.get(lang2ID))
							{
								PhoneticString lang2Form = database.getForm(lang2FormID);
								PhoneticStringAlignment alignment = LevenshteinAlignmentAlgorithm.constructAlignment(lang1Form, lang2Form);
								System.out.print(database.getConceptName(conceptID) + "\t");
								System.out.print(database.getLanguageCode(lang1ID) + "\t" + database.getLanguageCode(lang2ID) + "\t");
								System.out.print(database.getAnnotation("Word_Form", lang1FormID) + "\t" + database.getAnnotation("Word_Form", lang2FormID) + "\t");
								System.out.print(lang1Form.toString(symbolTable) + "\t" + lang2Form.toString(symbolTable) + "\t");
								System.out.println(alignment.alignmentScore + "\t" + alignment.normalizedDistanceScore);
							}
						}
					}
				}
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
