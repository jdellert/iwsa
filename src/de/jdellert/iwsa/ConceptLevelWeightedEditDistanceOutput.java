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

public class ConceptLevelWeightedEditDistanceOutput {
	public static void main(String[] args) {
		try {
			LexicalDatabase database = CLDFImport.loadDatabase(args[0], false);
			PhoneticSymbolTable symbolTable = database.getSymbolTable();
			int numPairs = 0;
			int numCognatePairs = 0;
			int[] cognatePairCorrespondenceCounts = new int[symbolTable.getSize() * symbolTable.getSize()];
			Arrays.fill(cognatePairCorrespondenceCounts, 1);
			double cognatePairCorrespondences = symbolTable.getSize() * symbolTable.getSize();
			System.err.print("Stage 1: Inference of global segment similarity matrix\n");
			System.err.print("  Step 1: Finding ED-based cognate candidates ...");
			for (int conceptID = 0; conceptID < database.getNumConcepts(); conceptID++) {
				List<List<Integer>> formsPerLang = database.getFormIDsForConceptPerLanguage(conceptID);
				for (int lang1ID = 0; lang1ID < database.getNumLanguages(); lang1ID++) {
					for (int lang2ID = lang1ID + 1; lang2ID < database.getNumLanguages(); lang2ID++) {
						for (int lang1FormID : formsPerLang.get(lang1ID)) {
							PhoneticString lang1Form = database.getForm(lang1FormID);
							for (int lang2FormID : formsPerLang.get(lang2ID)) {
								PhoneticString lang2Form = database.getForm(lang2FormID);
								PhoneticStringAlignment alignment = LevenshteinAlignmentAlgorithm
										.constructAlignment(lang1Form, lang2Form);
								numPairs++;
								if (alignment.normalizedAlignmentScore <= 0.35) {
									for (int pos = 0; pos < alignment.getLength(); pos++) {
										cognatePairCorrespondenceCounts[alignment.getSymbolPairIDAtPos(pos, symbolTable)]++;
									}
									numCognatePairs++;
								}
							}
						}
					}
				}
			}
			System.err.print(" done. Aligned " + numPairs + " form pairs, of which " + numCognatePairs
					+ " look like cognates (normalized edit distance < 0.35)\n");
			int[] randomPairCorrespondenceCounts = new int[symbolTable.getSize() * symbolTable.getSize()];
			Arrays.fill(randomPairCorrespondenceCounts, 100);
			double randomPairCorrespondences = symbolTable.getSize() * symbolTable.getSize() * 100;
			System.err.print("          Creating " + (numCognatePairs * 100)
					+ " random alignments to model the distribution in absence of correspondences ...");
			for (int i = 0; i < numCognatePairs * 100; i++) {
				PhoneticString form1 = database.getRandomForm();
				PhoneticString form2 = database.getRandomForm();
				PhoneticStringAlignment alignment = LevenshteinAlignmentAlgorithm.constructAlignment(form1, form2);

				for (int pos = 0; pos < alignment.getLength(); pos++) {
					randomPairCorrespondenceCounts[alignment.getSymbolPairIDAtPos(pos, symbolTable)]++;
					randomPairCorrespondences++;
				}
			}
			System.err.print(" done.");
			
			System.err.print("          Comparing the distributions of symbol pairs for PMI scores ...");
			for (int symbolPairID = 0; symbolPairID < symbolTable.getSize() * symbolTable.getSize(); symbolPairID++)
			{
				double cognateSymbolPairProbability = cognatePairCorrespondenceCounts[symbolPairID] / cognatePairCorrespondences;
				double randomSymbolPairProbability = randomPairCorrespondenceCounts[symbolPairID] / randomPairCorrespondences;
				double pmiScore = Math.log(cognateSymbolPairProbability/randomSymbolPairProbability);
				//if (cognatePairCorrespondenceCounts[symbolPairID] == 1 || pmiScore < 0.0) continue;
				System.err.println(symbolTable.toSymbol(symbolPairID / symbolTable.getSize()) + "\t" + symbolTable.toSymbol(symbolPairID % symbolTable.getSize()) + "\t" + cognateSymbolPairProbability + "\t" + randomSymbolPairProbability + "\t" + pmiScore);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
