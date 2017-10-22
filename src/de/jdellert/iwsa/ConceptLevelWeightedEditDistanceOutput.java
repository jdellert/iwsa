package de.jdellert.iwsa;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import de.jdellert.iwsa.align.LevenshteinAlignmentAlgorithm;
import de.jdellert.iwsa.align.PhoneticStringAlignment;
import de.jdellert.iwsa.corrmodel.CorrespondenceModel;
import de.jdellert.iwsa.data.CLDFImport;
import de.jdellert.iwsa.data.LexicalDatabase;
import de.jdellert.iwsa.sequence.PhoneticString;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;
import de.jdellert.iwsa.stat.CategoricalDistribution;
import de.jdellert.iwsa.stat.SmoothingMethod;

public class ConceptLevelWeightedEditDistanceOutput {
	public static void main(String[] args) {
		try {
			LexicalDatabase database = CLDFImport.loadDatabase(args[0], false);
			PhoneticSymbolTable symbolTable = database.getSymbolTable();
			int numPairs = 0;
			int numCognatePairs = 0;
			CategoricalDistribution cognatePairCorrespondenceDist = new CategoricalDistribution(symbolTable.getSize() * symbolTable.getSize(), SmoothingMethod.LAPLACE);
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
										cognatePairCorrespondenceDist.addObservation(alignment.getSymbolPairIDAtPos(pos, symbolTable));
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
			CategoricalDistribution randomPairCorrespondenceDist = new CategoricalDistribution(symbolTable.getSize() * symbolTable.getSize(), SmoothingMethod.LAPLACE);
			System.err.print("          Creating " + (numCognatePairs * 10)
					+ " random alignments to model the distribution in absence of correspondences ...");
			for (int i = 0; i < numCognatePairs * 100; i++) {
				PhoneticString form1 = database.getRandomForm();
				PhoneticString form2 = database.getRandomForm();
				PhoneticStringAlignment alignment = LevenshteinAlignmentAlgorithm.constructAlignment(form1, form2);
				for (int pos = 0; pos < alignment.getLength(); pos++) {
					randomPairCorrespondenceDist.addObservation(alignment.getSymbolPairIDAtPos(pos, symbolTable));
				}
			}
			System.err.print(" done.");
			
			System.err.print("          Comparing the distributions of symbol pairs for PMI scores ...");
			CorrespondenceModel globalCorr = new CorrespondenceModel(symbolTable);
			for (int symbolPairID = 0; symbolPairID < symbolTable.getSize() * symbolTable.getSize(); symbolPairID++)
			{
				double cognateSymbolPairProbability = cognatePairCorrespondenceDist.getProb(symbolPairID);
				double randomSymbolPairProbability = randomPairCorrespondenceDist.getProb(symbolPairID);
				double pmiScore = Math.log(cognateSymbolPairProbability/randomSymbolPairProbability);
				if (Math.abs(pmiScore) >= 0.1)
				{
					globalCorr.setScore(symbolPairID, pmiScore);
					//System.err.println(symbolTable.toSymbol(symbolPairID / symbolTable.getSize()) + "\t" + symbolTable.toSymbol(symbolPairID % symbolTable.getSize()) + "\t" + 
					//		cognateSymbolPairProbability + "\t" + randomSymbolPairProbability + "\t" + pmiScore);
				}
			}
			System.err.print(" done.");
			
			System.err.print("  Step 2: Finding WED-based cognate candidates ...");
			numPairs = 0;
			numCognatePairs = 0;
			
			
			System.err.print(" done. Aligned " + numPairs + " form pairs, of which " + numCognatePairs
					+ " look like cognates (normalized aligment score < 0.35)\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
