package de.jdellert.iwsa.corrmodel;

import java.util.List;
import java.util.Map;

import de.jdellert.iwsa.ConceptLevelWeightedEditDistanceOutput;
import de.jdellert.iwsa.align.LevenshteinAlignmentAlgorithm;
import de.jdellert.iwsa.align.NeedlemanWunschAlgorithm;
import de.jdellert.iwsa.align.PhoneticStringAlignment;
import de.jdellert.iwsa.data.LexicalDatabase;
import de.jdellert.iwsa.sequence.PhoneticString;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;
import de.jdellert.iwsa.stat.CategoricalDistribution;
import de.jdellert.iwsa.stat.SmoothingMethod;
import de.jdellert.iwsa.util.io.Formatting;

public class CorrespondenceModelInference {
	public static boolean VERBOSE = true;
	
	public static CorrespondenceModel inferGlobalCorrespondenceModel(LexicalDatabase database,
			PhoneticSymbolTable symbolTable) {
		CategoricalDistribution randomPairCorrespondenceDist = new CategoricalDistribution(
				symbolTable.getSize() * symbolTable.getSize(), SmoothingMethod.LAPLACE);
		int numRandomPairs = database.getNumConcepts() * database.getNumLanguages() * database.getNumLanguages();
		System.err.print("  Step 1: Simulating non-cognates by means of " + numRandomPairs + " random alignments ...");
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
										.constructAlignment(lang1Form, lang2Form, globalCorr, globalCorr, globalCorr);
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
			System.err.print(" done. " + numCognatePairs
					+ " form pairs look like cognates (normalized aligment score < 0.35)\n");

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
			PhoneticSymbolTable symbolTable, int[] relevantLangIDs, CorrespondenceModel globalCorr) {
		CorrespondenceModel[][] localCorrModels = new CorrespondenceModel[database.getNumLanguages()][database
				.getNumLanguages()];
		for (int langID : relevantLangIDs) {
			System.err.println("Storing localCorrModels[" + langID + "][" + langID + "]");
			localCorrModels[langID][langID] = inferSelfSimilarityModel(database, symbolTable, langID, globalCorr);
		}
		for (int lang1ID : relevantLangIDs) {
			for (int lang2ID : relevantLangIDs) {
				if (lang1ID == lang2ID)
					continue;
				System.err.println("Storing localCorrModels[" + lang1ID + "][" + lang2ID + "]");
				localCorrModels[lang1ID][lang2ID] = inferCorrModelForPair(database, symbolTable, lang1ID, lang2ID,
						globalCorr, localCorrModels[lang1ID][lang1ID], localCorrModels[lang2ID][lang2ID]);
			}
		}
		return localCorrModels;
	}

	public static CorrespondenceModel inferSelfSimilarityModel(LexicalDatabase database,
			PhoneticSymbolTable symbolTable, int langID, CorrespondenceModel globalCorr) {
		System.err.print("  Self-similarity for language " + database.getLanguageCode(langID) + ":\n");

		CategoricalDistribution randomCorrespondenceDistForPair = new CategoricalDistribution(
				symbolTable.getSize() * symbolTable.getSize(), SmoothingMethod.LAPLACE);
		System.err.print("    Step 1: " + 100000
				+ " random alignments to model the distribution in absence of correspondences ...");
		for (int i = 0; i < 100000; i++) {
			PhoneticString form1 = database.getRandomFormForLanguage(langID);
			PhoneticString form2 = database.getRandomFormForLanguage(langID);
			PhoneticStringAlignment alignment = NeedlemanWunschAlgorithm.constructAlignment(form1, form2, globalCorr,
					globalCorr, globalCorr);
			for (int pos = 0; pos < alignment.getLength(); pos++) {
				randomCorrespondenceDistForPair.addObservation(alignment.getSymbolPairIDAtPos(pos, symbolTable));
			}
		}
		System.err.print(" done.\n");
		// first iteration: use global sound correspondences
		CategoricalDistribution cognateCorrespondenceDistForPair = new CategoricalDistribution(
				symbolTable.getSize() * symbolTable.getSize(), SmoothingMethod.LAPLACE);
		System.err.print("    Step 2: Self-alignment based on global WED ...");
		for (int conceptID = 0; conceptID < database.getNumConcepts(); conceptID++) {
			List<List<Integer>> formsPerLang = database.getFormIDsForConceptPerLanguage(conceptID);
			for (int langFormID : formsPerLang.get(langID)) {
				PhoneticString langForm = database.getForm(langFormID);
				PhoneticStringAlignment alignment = NeedlemanWunschAlgorithm.constructAlignment(langForm, langForm,
						globalCorr, globalCorr, globalCorr);
				for (int pos = 0; pos < alignment.getLength(); pos++) {
					cognateCorrespondenceDistForPair.addObservation(alignment.getSymbolPairIDAtPos(pos, symbolTable));
				}
			}
		}
		System.err.print(" done.\n");

		System.err.print("          Comparing the distributions of symbol pairs to reestimate PMI scores ...");
		CorrespondenceModel localCorr = new CorrespondenceModel(symbolTable);
		for (int symbolPairID = 0; symbolPairID < symbolTable.getSize() * symbolTable.getSize(); symbolPairID++) {
			double cognateSymbolPairProbability = cognateCorrespondenceDistForPair.getProb(symbolPairID);
			double randomSymbolPairProbability = randomCorrespondenceDistForPair.getProb(symbolPairID);
			double pmiScore = Math.log(cognateSymbolPairProbability / randomSymbolPairProbability);
			localCorr.setScore(symbolPairID, Math.max(globalCorr.getScore(symbolPairID),pmiScore));
			//localCorr.setScore(symbolPairID, (globalCorr.getScore(symbolPairID) + pmiScore) / 2);
		}
		System.err.print(" done.\n");
		return localCorr;
	}

	public static CorrespondenceModel inferCorrModelForPair(LexicalDatabase database, PhoneticSymbolTable symbolTable,
			int lang1ID, int lang2ID, CorrespondenceModel globalCorr, CorrespondenceModel lang1SelfCorr,
			CorrespondenceModel lang2SelfCorr) {
		System.err
				.print("  Pair " + database.getLanguageCode(lang1ID) + "/" + database.getLanguageCode(lang2ID) + ":\n");

		CategoricalDistribution randomCorrespondenceDistForPair = new CategoricalDistribution(
				symbolTable.getSize() * symbolTable.getSize(), SmoothingMethod.LAPLACE);
		System.err.print("    Step 1: " + 100000
				+ " random alignments to model the distribution in absence of correspondences ...");
		for (int i = 0; i < 100000; i++) {
			PhoneticString form1 = database.getRandomFormForLanguage(lang1ID);
			PhoneticString form2 = database.getRandomFormForLanguage(lang2ID);
			PhoneticStringAlignment alignment = NeedlemanWunschAlgorithm.constructAlignment(form1, form2, globalCorr,
					globalCorr, globalCorr);
			for (int pos = 0; pos < alignment.getLength(); pos++) {
				randomCorrespondenceDistForPair.addObservation(alignment.getSymbolPairIDAtPos(pos, symbolTable));
			}
		}
		System.err.print(" done.\n");

		// first iteration: use global sound correspondences
		CategoricalDistribution cognateCorrespondenceDistForPair = new CategoricalDistribution(
				symbolTable.getSize() * symbolTable.getSize(), SmoothingMethod.LAPLACE);
		int numPairs = 0;
		int numCognatePairs = 0;
		System.err.print("    Step 2: Finding cognate candidates based on global WED ...");
		for (int conceptID = 0; conceptID < database.getNumConcepts(); conceptID++) {
			List<List<Integer>> formsPerLang = database.getFormIDsForConceptPerLanguage(conceptID);
			for (int lang1FormID : formsPerLang.get(lang1ID)) {
				PhoneticString lang1Form = database.getForm(lang1FormID);
				for (int lang2FormID : formsPerLang.get(lang2ID)) {
					PhoneticString lang2Form = database.getForm(lang2FormID);
					PhoneticStringAlignment alignment = NeedlemanWunschAlgorithm.constructAlignment(lang1Form,
							lang2Form, globalCorr, globalCorr, globalCorr);
					numPairs++;
					if (alignment.normalizedDistanceScore <= 0.35) {
						for (int pos = 0; pos < alignment.getLength(); pos++) {
							cognateCorrespondenceDistForPair
									.addObservation(alignment.getSymbolPairIDAtPos(pos, symbolTable));
						}
						numCognatePairs++;
					}
				}
			}
		}
		System.err.print(" done. Aligned " + numPairs + " form pairs, of which " + numCognatePairs
				+ " look like cognates (weighted edit distance <= 0.35)\n");

		System.err.print("          Comparing the distributions of symbol pairs to reestimate PMI scores ...");
		CorrespondenceModel localCorr = new CorrespondenceModel(symbolTable);
		for (int symbolPairID = 0; symbolPairID < symbolTable.getSize() * symbolTable.getSize(); symbolPairID++) {
			double cognateSymbolPairProbability = cognateCorrespondenceDistForPair.getProb(symbolPairID);
			double randomSymbolPairProbability = randomCorrespondenceDistForPair.getProb(symbolPairID);
			double pmiScore = Math.log(cognateSymbolPairProbability / randomSymbolPairProbability);
			localCorr.setScore(symbolPairID, (globalCorr.getScore(symbolPairID) + pmiScore) / 2);
		}
		System.err.print(" done.\n");

		int numLocalInferenceIterations = 10;
		System.err.print("    Step 3: Reestimation based on Needleman-Wunsch (" + numLocalInferenceIterations
				+ " iterations)\n");

		for (int iteration = 0; iteration < numLocalInferenceIterations; iteration++) {
			System.err.print("    Iteration 0" + (iteration + 1) + ": Finding WED-based cognate candidates ...");
			numPairs = 0;
			numCognatePairs = 0;
			cognateCorrespondenceDistForPair = new CategoricalDistribution(
					symbolTable.getSize() * symbolTable.getSize(), SmoothingMethod.LAPLACE);
			for (int conceptID = 0; conceptID < database.getNumConcepts(); conceptID++) {
				List<List<Integer>> formsPerLang = database.getFormIDsForConceptPerLanguage(conceptID);
				for (int lang1FormID : formsPerLang.get(lang1ID)) {
					PhoneticString lang1Form = database.getForm(lang1FormID);
					for (int lang2FormID : formsPerLang.get(lang2ID)) {
						PhoneticString lang2Form = database.getForm(lang2FormID);
						PhoneticStringAlignment alignment = NeedlemanWunschAlgorithm.constructAlignment(lang1Form,
								lang2Form, localCorr, lang1SelfCorr, lang2SelfCorr);
						numPairs++;
						if (alignment.normalizedDistanceScore <= 0.35) {
							for (int pos = 0; pos < alignment.getLength(); pos++) {
								cognateCorrespondenceDistForPair
										.addObservation(alignment.getSymbolPairIDAtPos(pos, symbolTable));
							}
							numCognatePairs++;
						}
					}
				}
			}

			System.err.print(" done. " + numCognatePairs
					+ " form pairs look like cognates (normalized distance score <= 0.35)\n");

			// randomCorrespondenceDistForPair = new CategoricalDistribution(
			// symbolTable.getSize() * symbolTable.getSize(), SmoothingMethod.LAPLACE);
			// System.err.print(" Creating " + (numCognatePairs * 10)
			// + " random alignments to model the distribution in absence of correspondences
			// ...");
			// for (int i = 0; i < numCognatePairs * 10; i++) {
			// PhoneticString form1 = database.getRandomFormForLanguage(lang1ID);
			// PhoneticString form2 = database.getRandomFormForLanguage(lang2ID);
			// PhoneticStringAlignment alignment =
			// NeedlemanWunschAlgorithm.constructAlignment(form1, form2,
			// localCorr);
			// for (int pos = 0; pos < alignment.getLength(); pos++) {
			// randomCorrespondenceDistForPair
			// .addObservation(alignment.getSymbolPairIDAtPos(pos, symbolTable));
			// }
			// }
			// System.err.print(" done.\n");

			System.err.print("          Comparing the distributions of symbol pairs to reestimate PMI scores ...");
			localCorr = new CorrespondenceModel(symbolTable);
			for (int symbolPairID = 0; symbolPairID < symbolTable.getSize() * symbolTable.getSize(); symbolPairID++) {
				double cognateSymbolPairProbability = cognateCorrespondenceDistForPair.getProb(symbolPairID);
				double randomSymbolPairProbability = randomCorrespondenceDistForPair.getProb(symbolPairID);
				double pmiScore = Math.log(cognateSymbolPairProbability / randomSymbolPairProbability);
				double avgScore = (globalCorr.getScore(symbolPairID) + pmiScore) / 2;
				avgScore = globalCorr.getScore(symbolPairID);
				if (pmiScore > avgScore) avgScore = pmiScore;
				if (Math.abs(avgScore) > 0.1)
				{
					localCorr.setScore(symbolPairID, avgScore);
					if (VERBOSE) System.err.println(database.getLanguageCode(lang1ID) + "-" + database.getLanguageCode(lang2ID)
							+ " correspondence for " + symbolTable.toSymbolPair(symbolPairID) + ": "
							+ Formatting.str3f(globalCorr.getScore(symbolPairID)) + "\t" + Formatting.str3f(pmiScore) + "\t"
							+ Formatting.str3f(localCorr.getScore(symbolPairID)) + "\t"
							+ Formatting.str3f(cognateSymbolPairProbability) + " (" + (int) cognateCorrespondenceDistForPair.getObservationCount(symbolPairID) + "/" + (int) cognateCorrespondenceDistForPair.getObservationCountsSum() + ")\t"
							+ Formatting.str3f(randomSymbolPairProbability) + " (" + (int) randomCorrespondenceDistForPair.getObservationCount(symbolPairID) + "/" + (int) randomCorrespondenceDistForPair.getObservationCountsSum() + ")\t");
				}
			}
			System.err.print(" done.\n");

			if (VERBOSE) ConceptLevelWeightedEditDistanceOutput.distanceOutput(database, symbolTable, lang1ID, lang2ID, globalCorr, localCorr);
		}
		return localCorr;
	}

}
