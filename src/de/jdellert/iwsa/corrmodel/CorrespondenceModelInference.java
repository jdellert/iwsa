package de.jdellert.iwsa.corrmodel;

import java.util.List;
import java.util.Map;

import de.jdellert.iwsa.align.LevenshteinAlignmentAlgorithm;
import de.jdellert.iwsa.align.NeedlemanWunschAlgorithm;
import de.jdellert.iwsa.align.PhoneticStringAlignment;
import de.jdellert.iwsa.data.LexicalDatabase;
import de.jdellert.iwsa.sequence.PhoneticString;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;
import de.jdellert.iwsa.stat.CategoricalDistribution;
import de.jdellert.iwsa.stat.SmoothingMethod;

public class CorrespondenceModelInference {
	public static CorrespondenceModel inferGlobalCorrespondenceModel(LexicalDatabase database,
			PhoneticSymbolTable symbolTable) {
		CategoricalDistribution randomPairCorrespondenceDist = new CategoricalDistribution(
				symbolTable.getSize() * symbolTable.getSize(), SmoothingMethod.LAPLACE);
		int numRandomPairs = database.getNumConcepts() * database.getNumLanguages() * database.getNumLanguages();
		System.err.print("  Step 1: Simulating non-cognates by means of " +  numRandomPairs + " random alignments ...");
		for (int i = 0; i < numRandomPairs; i++) {
			PhoneticString form1 = database.getRandomForm();
			PhoneticString form2 = database.getRandomForm();
			PhoneticStringAlignment alignment = LevenshteinAlignmentAlgorithm.constructAlignment(form1, form2);
			for (int pos = 0; pos < alignment.getLength(); pos++) {
				randomPairCorrespondenceDist.addObservation(alignment.getSymbolPairIDAtPos(pos, symbolTable));
			}
		}
		System.err.print(" done.\n");
		
		int numPairs = 0;
		int numCognatePairs = 0;
		CategoricalDistribution cognatePairCorrespondenceDist = new CategoricalDistribution(
				symbolTable.getSize() * symbolTable.getSize(), SmoothingMethod.LAPLACE);
		
		System.err.print("  Step 2: Finding ED-based cognate candidates ...");
		for (int conceptID = 0; conceptID < database.getNumConcepts(); conceptID++) {
			List<List<Integer>> formsPerLang = database.getFormIDsForConceptPerLanguage(conceptID);
			for (int lang1ID = 0; lang1ID < database.getNumLanguages(); lang1ID++) {
				for (int lang2ID = 0; lang2ID < database.getNumLanguages(); lang2ID++) {
					for (int lang1FormID : formsPerLang.get(lang1ID)) {
						PhoneticString lang1Form = database.getForm(lang1FormID);
						for (int lang2FormID : formsPerLang.get(lang2ID)) {
							PhoneticString lang2Form = database.getForm(lang2FormID);
							PhoneticStringAlignment alignment = LevenshteinAlignmentAlgorithm
									.constructAlignment(lang1Form, lang2Form);
							numPairs++;
							if (alignment.normalizedDistanceScore <= 0.35) {
								for (int pos = 0; pos < alignment.getLength(); pos++) {
									cognatePairCorrespondenceDist
											.addObservation(alignment.getSymbolPairIDAtPos(pos, symbolTable));
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

		System.err.print("          Comparing the distributions of symbol pairs for PMI scores ...");
		CorrespondenceModel globalCorr = new CorrespondenceModel(symbolTable);
		for (int symbolPairID = 0; symbolPairID < symbolTable.getSize() * symbolTable.getSize(); symbolPairID++) {
			double cognateSymbolPairProbability = cognatePairCorrespondenceDist.getProb(symbolPairID);
			double randomSymbolPairProbability = randomPairCorrespondenceDist.getProb(symbolPairID);
			double pmiScore = Math.log(cognateSymbolPairProbability / randomSymbolPairProbability);
			globalCorr.setScore(symbolPairID, pmiScore);
		}
		System.err.print(" done.\n");

		int numGlobalInferenceIterations = 3;
		System.err.print(
				"  Step 3: Reestimation based on Needleman-Wunsch (" + numGlobalInferenceIterations + " iterations)\n");
		for (int iteration = 0; iteration < numGlobalInferenceIterations; iteration++) {
			cognatePairCorrespondenceDist = new CategoricalDistribution(symbolTable.getSize() * symbolTable.getSize(),
					SmoothingMethod.LAPLACE);
			System.err.print("    Iteration 0" + (iteration + 1) + ": Finding WED-based cognate candidates ...");
			numPairs = 0;
			numCognatePairs = 0;
			for (int conceptID = 0; conceptID < database.getNumConcepts(); conceptID++) {
				List<List<Integer>> formsPerLang = database.getFormIDsForConceptPerLanguage(conceptID);
				for (int lang1ID = 0; lang1ID < database.getNumLanguages(); lang1ID++) {
					for (int lang2ID = 0; lang2ID < database.getNumLanguages(); lang2ID++) {
						for (int lang1FormID : formsPerLang.get(lang1ID)) {
							PhoneticString lang1Form = database.getForm(lang1FormID);
							for (int lang2FormID : formsPerLang.get(lang2ID)) {
								PhoneticString lang2Form = database.getForm(lang2FormID);
								PhoneticStringAlignment alignment = NeedlemanWunschAlgorithm
										.constructAlignment(lang1Form, lang2Form, globalCorr);
								numPairs++;
								if (alignment.normalizedDistanceScore <= 0.7) {
									for (int pos = 0; pos < alignment.getLength(); pos++) {
										cognatePairCorrespondenceDist
												.addObservation(alignment.getSymbolPairIDAtPos(pos, symbolTable));
									}
									numCognatePairs++;
								}
							}
						}
					}
				}
			}
			System.err.print(
					" done. " + numCognatePairs + " form pairs look like cognates (normalized aligment score < 0.7)\n");

			System.err.print("          Comparing the distributions of symbol pairs to reestimate PMI scores ...");
			globalCorr = new CorrespondenceModel(symbolTable);
			for (int symbolPairID = 0; symbolPairID < symbolTable.getSize() * symbolTable.getSize(); symbolPairID++) {
				double cognateSymbolPairProbability = cognatePairCorrespondenceDist.getProb(symbolPairID);
				double randomSymbolPairProbability = randomPairCorrespondenceDist.getProb(symbolPairID);
				double pmiScore = Math.log(cognateSymbolPairProbability / randomSymbolPairProbability);
				globalCorr.setScore(symbolPairID, pmiScore);
			}
			System.err.print(" done.\n");
		}

		return globalCorr;
	}
	
	public static CorrespondenceModel[][] inferLocalCorrespondenceModels(LexicalDatabase database,
			PhoneticSymbolTable symbolTable, CorrespondenceModel globalCorrespondenceModel)
	{
		CorrespondenceModel[][] models = new CorrespondenceModel[database.getNumLanguages()][database.getNumLanguages()];
		//TODO
		return models;
	}
}
