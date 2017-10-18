package de.jdellert.iwsa;

import java.io.IOException;
import java.util.List;

import de.jdellert.iwsa.align.LevenshteinAlignmentAlgorithm;
import de.jdellert.iwsa.align.PhoneticStringAlignment;
import de.jdellert.iwsa.data.CLDFImport;
import de.jdellert.iwsa.data.LexicalDatabase;
import de.jdellert.iwsa.sequence.PhoneticString;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;

public class ConceptLevelWeightedEditDistanceOutput {
	 public static void main(String[] args)
	 {
			try {
				LexicalDatabase database = CLDFImport.loadDatabase(args[0], false);
				PhoneticSymbolTable symbolTable = database.getSymbolTable();
				int numPairs = 0;
				int numCognatePairs = 0;
				System.err.println("Stage 1: inference of global segment similarity matrix.");
				System.err.print("  Step 1: finding ED-based cognate candidates ...");
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
									numPairs++;
									if (alignment.normalizedAlignmentScore <= 0.3) numCognatePairs++;
								}
							}
						}
					}
				}
				System.err.println(" done. Aligned " + numPairs + " form pairs, of which " + numCognatePairs + " look like cognates (normalized edit distance < 0.3)");
				
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
