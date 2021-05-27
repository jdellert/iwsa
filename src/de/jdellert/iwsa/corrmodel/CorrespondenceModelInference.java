package de.jdellert.iwsa.corrmodel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.jdellert.iwsa.ConceptLevelWeightedEditDistanceOutput;
import de.jdellert.iwsa.align.InformationWeightedSequenceAlignment;
import de.jdellert.iwsa.align.LevenshteinAlignmentAlgorithm;
import de.jdellert.iwsa.align.NeedlemanWunschAlgorithm;
import de.jdellert.iwsa.align.PhoneticStringAlignment;
import de.jdellert.iwsa.data.LexicalDatabase;
import de.jdellert.iwsa.infomodel.InformationModel;
import de.jdellert.iwsa.sequence.PhoneticString;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;
import de.jdellert.iwsa.stat.CategoricalDistribution;
import de.jdellert.iwsa.util.io.Formatting;
import de.tuebingen.sfs.cldfjava.data.CLDFForm;
import de.tuebingen.sfs.cldfjava.data.CLDFWordlistDatabase;

public class CorrespondenceModelInference {
	public static boolean VERBOSE = false;

	public static double COGNACY_CANDIDATE_WED_THRESHOLD = 1.2;
	public static int NUM_GLOBAL_CORR_REESTIMATIONS = 3;
	public static int NUM_LOCAL_CORR_REESTIMATIONS = 5;
	public static int NUM_RANDOM_PAIRS_LOCAL = 100000;
	
	public static double LOCAL_PMI_RATIO = 0.5;
	
	public static CorrespondenceModel inferGlobalCorrespondenceModel(LexicalDatabase database,
			PhoneticSymbolTable symbolTable) {
		return inferGlobalCorrespondenceModel(database, symbolTable, null);
	}

	public static CorrespondenceModel inferGlobalCorrespondenceModel(LexicalDatabase database,
			PhoneticSymbolTable symbolTable, InformationModel[] infoModels) {
		CategoricalDistribution randomPairCorrespondenceDist = new CategoricalDistribution(
				symbolTable.getSize() * symbolTable.getSize());
		int numRandomPairs = database.getNumConcepts() * database.getNumLanguages() * database.getNumLanguages();
		System.err.print("  Step 1: Simulating non-cognates by means of " + numRandomPairs + " random alignments ...");
		for (int i = 0; i < numRandomPairs; i++) {
			int randomLang1 = (int) (Math.random() * database.getNumLanguages());
			int randomLang2 = (int) (Math.random() * database.getNumLanguages());
			PhoneticString form1 = database.getRandomFormForLanguage(randomLang1);
			PhoneticString form2 = database.getRandomFormForLanguage(randomLang2);
			PhoneticStringAlignment alignment = LevenshteinAlignmentAlgorithm.constructAlignment(form1, form2);
			double[] infoScores = new double[alignment.getLength()];
			if (infoModels == null) {
				Arrays.fill(infoScores, 1.0);
			}
			else {
				infoScores = InformationWeightedSequenceAlignment.combinedInfoScoresForAlignment(alignment, infoModels[randomLang1], infoModels[randomLang2]);
			}
			for (int pos = 0; pos < alignment.getLength(); pos++) {
				randomPairCorrespondenceDist.addObservation(alignment.getSymbolPairIDAtPos(pos, symbolTable), infoScores[pos]);
			}
		}
		System.err.print(" done.\n");

		int numPairs = 0;
		int numCognatePairs = 0;
		CategoricalDistribution cognatePairCorrespondenceDist = new CategoricalDistribution(
				symbolTable.getSize() * symbolTable.getSize());

		System.err.print("  Step 2: Finding ED-based cognate candidates ...");
		for (int conceptID = 0; conceptID < database.getNumConcepts(); conceptID++) {
			List<List<Integer>> formsPerLang = database.getFormIDsForConceptPerLanguage(conceptID);
			for (int lang1ID = 0; lang1ID < database.getNumLanguages(); lang1ID++) {
				for (int lang2ID = 0; lang2ID < database.getNumLanguages(); lang2ID++) {
					for (int lang1FormID : formsPerLang.get(lang1ID)) {
						PhoneticString lang1Form = database.getForm(lang1FormID);
						for (int lang2FormID : formsPerLang.get(lang2ID)) {
							PhoneticString lang2Form = database.getForm(lang2FormID);
							PhoneticStringAlignment alignment = LevenshteinAlignmentAlgorithm.constructAlignment(lang1Form, lang2Form);
							numPairs++;
							double[] infoScores = new double[alignment.getLength()];
							if (infoModels == null) {
								Arrays.fill(infoScores, 1.0);
							}
							else {
								infoScores = InformationWeightedSequenceAlignment.combinedInfoScoresForAlignment(alignment, infoModels[lang1ID], infoModels[lang2ID]);
							}		
							if (alignment.normalizedDistanceScore <= 0.35) {
								for (int pos = 0; pos < alignment.getLength(); pos++) {
									cognatePairCorrespondenceDist
											.addObservation(alignment.getSymbolPairIDAtPos(pos, symbolTable), infoScores[pos]);
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

		System.err.print(
				"  Step 3: Reestimation based on Needleman-Wunsch (" + NUM_GLOBAL_CORR_REESTIMATIONS + " iterations)\n");
		for (int iteration = 0; iteration < NUM_GLOBAL_CORR_REESTIMATIONS; iteration++) {
			cognatePairCorrespondenceDist = new CategoricalDistribution(symbolTable.getSize() * symbolTable.getSize());
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
								PhoneticStringAlignment alignment = NeedlemanWunschAlgorithm.constructAlignment(
										lang1Form, lang2Form, globalCorr, globalCorr, globalCorr, globalCorr);
								numPairs++;
								double[] infoScores = new double[alignment.getLength()];
								if (infoModels == null) {
									Arrays.fill(infoScores, 1.0);
								}
								else {
									infoScores = InformationWeightedSequenceAlignment.combinedInfoScoresForAlignment(alignment, infoModels[lang1ID], infoModels[lang2ID]);
								}				
								if (alignment.normalizedDistanceScore <= 0.6) {
									for (int pos = 0; pos < alignment.getLength(); pos++) {
										cognatePairCorrespondenceDist
												.addObservation(alignment.getSymbolPairIDAtPos(pos, symbolTable), infoScores[pos]);
									}
									numCognatePairs++;
								}
							}
						}
					}
				}
			}
			System.err.print(
					" done. " + numCognatePairs + " form pairs look like cognates (normalized aligment score < 0.6)\n");

			System.err.print("          Comparing the distributions of symbol pairs to reestimate PMI scores ...");
			globalCorr = new CorrespondenceModel(symbolTable);
			for (int symbolPairID = 0; symbolPairID < symbolTable.getSize() * symbolTable.getSize(); symbolPairID++) {
				double cognateSymbolPairProbability = cognatePairCorrespondenceDist.getProb(symbolPairID);
				double randomSymbolPairProbability = randomPairCorrespondenceDist.getProb(symbolPairID);
				double pmiScore = Math.log(cognateSymbolPairProbability / randomSymbolPairProbability);
				globalCorr.setScore(symbolPairID, pmiScore);
				if (VERBOSE)
					System.err.println(" Global correspondence for " + symbolTable.toSymbolPair(symbolPairID) + " in iteration "
							+ iteration + ": " + Formatting.str3f(globalCorr.getScore(symbolPairID)) + "\t"
							+ Formatting.str3f(pmiScore) + "\t"
							+ "\t" + Formatting.str12f(cognateSymbolPairProbability) + " ("
							+ (int) cognatePairCorrespondenceDist.getObservationCount(symbolPairID) + "/"
							+ (int) cognatePairCorrespondenceDist.getObservationCountsSum() + ")\t"
							+ Formatting.str12f(randomSymbolPairProbability) + " ("
							+ (int) randomPairCorrespondenceDist.getObservationCount(symbolPairID) + "/"
							+ (int) randomPairCorrespondenceDist.getObservationCountsSum() + ")\t");
			}
			System.err.print(" done.\n");
		}

		return globalCorr;
	}

	public static CorrespondenceModel inferGlobalCorrespondenceModel(CLDFWordlistDatabase database,
																	 PhoneticSymbolTable symbolTable,
																	 InformationModel[] infoModels) {
		CategoricalDistribution randomPairCorrespondenceDist = new CategoricalDistribution(
				symbolTable.getSize() * symbolTable.getSize());
		int numRandomPairs = database.getConceptMap().size() * database.getLanguageMap().size() *
				database.getLanguageMap().size();
		System.err.print("  Step 1: Simulating non-cognates by means of " + numRandomPairs + " random alignments ...");
		ArrayList<String> langIDsAsList = new ArrayList<>(database.getLanguageMap().keySet());
		for (int i = 0; i < numRandomPairs; i++) {
			int randomLangIdx1 = (int) (Math.random() * database.getLanguageMap().size());
			int randomLangIdx2 = (int) (Math.random() * database.getLanguageMap().size());
			String randomLangId1 = langIDsAsList.get(randomLangIdx1);
			String randomLangId2 = langIDsAsList.get(randomLangIdx2);
			CLDFForm form1inCLDF = database.getRandomFormForLanguage(randomLangId1);
			CLDFForm form2inCLDF = database.getRandomFormForLanguage(randomLangId2);
			// get the segments from the CLDFForm, encode them as ints and construct PhoneticString objects
			PhoneticString form1 = new PhoneticString(symbolTable.encode(form1inCLDF.getSegments()));
			PhoneticString form2 = new PhoneticString(symbolTable.encode(form2inCLDF.getSegments()));
			PhoneticStringAlignment alignment = LevenshteinAlignmentAlgorithm.constructAlignment(form1, form2);
			double[] infoScores = new double[alignment.getLength()];
			if (infoModels == null) {
				Arrays.fill(infoScores, 1.0);
			}
			else {
				infoScores = InformationWeightedSequenceAlignment.combinedInfoScoresForAlignment(alignment, infoModels[randomLangIdx1], infoModels[randomLangIdx2]);
			}
			for (int pos = 0; pos < alignment.getLength(); pos++) {
				randomPairCorrespondenceDist.addObservation(alignment.getSymbolPairIDAtPos(pos, symbolTable), infoScores[pos]);
			}
		}
		System.err.print(" done.\n");

		int numPairs = 0;
		int numCognatePairs = 0;
		CategoricalDistribution cognatePairCorrespondenceDist = new CategoricalDistribution(
				symbolTable.getSize() * symbolTable.getSize());


		////////////////////////////////////////////////////////////////////////////////////

		System.err.print("  Step 2: Finding ED-based cognate candidates ...");
		for (int conceptID = 0; conceptID < database.getNumConcepts(); conceptID++) {
			List<List<Integer>> formsPerLang = database.getFormIDsForConceptPerLanguage(conceptID);
			for (int lang1ID = 0; lang1ID < database.getNumLanguages(); lang1ID++) {
				for (int lang2ID = 0; lang2ID < database.getNumLanguages(); lang2ID++) {
					for (int lang1FormID : formsPerLang.get(lang1ID)) {
						PhoneticString lang1Form = database.getForm(lang1FormID);
						for (int lang2FormID : formsPerLang.get(lang2ID)) {
							PhoneticString lang2Form = database.getForm(lang2FormID);
							PhoneticStringAlignment alignment = LevenshteinAlignmentAlgorithm.constructAlignment(lang1Form, lang2Form);
							numPairs++;
							double[] infoScores = new double[alignment.getLength()];
							if (infoModels == null) {
								Arrays.fill(infoScores, 1.0);
							}
							else {
								infoScores = InformationWeightedSequenceAlignment.combinedInfoScoresForAlignment(alignment, infoModels[lang1ID], infoModels[lang2ID]);
							}
							if (alignment.normalizedDistanceScore <= 0.35) {
								for (int pos = 0; pos < alignment.getLength(); pos++) {
									cognatePairCorrespondenceDist
											.addObservation(alignment.getSymbolPairIDAtPos(pos, symbolTable), infoScores[pos]);
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

		System.err.print(
				"  Step 3: Reestimation based on Needleman-Wunsch (" + NUM_GLOBAL_CORR_REESTIMATIONS + " iterations)\n");
		for (int iteration = 0; iteration < NUM_GLOBAL_CORR_REESTIMATIONS; iteration++) {
			cognatePairCorrespondenceDist = new CategoricalDistribution(symbolTable.getSize() * symbolTable.getSize());
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
								PhoneticStringAlignment alignment = NeedlemanWunschAlgorithm.constructAlignment(
										lang1Form, lang2Form, globalCorr, globalCorr, globalCorr, globalCorr);
								numPairs++;
								double[] infoScores = new double[alignment.getLength()];
								if (infoModels == null) {
									Arrays.fill(infoScores, 1.0);
								}
								else {
									infoScores = InformationWeightedSequenceAlignment.combinedInfoScoresForAlignment(alignment, infoModels[lang1ID], infoModels[lang2ID]);
								}
								if (alignment.normalizedDistanceScore <= 0.6) {
									for (int pos = 0; pos < alignment.getLength(); pos++) {
										cognatePairCorrespondenceDist
												.addObservation(alignment.getSymbolPairIDAtPos(pos, symbolTable), infoScores[pos]);
									}
									numCognatePairs++;
								}
							}
						}
					}
				}
			}
			System.err.print(
					" done. " + numCognatePairs + " form pairs look like cognates (normalized aligment score < 0.6)\n");

			System.err.print("          Comparing the distributions of symbol pairs to reestimate PMI scores ...");
			globalCorr = new CorrespondenceModel(symbolTable);
			for (int symbolPairID = 0; symbolPairID < symbolTable.getSize() * symbolTable.getSize(); symbolPairID++) {
				double cognateSymbolPairProbability = cognatePairCorrespondenceDist.getProb(symbolPairID);
				double randomSymbolPairProbability = randomPairCorrespondenceDist.getProb(symbolPairID);
				double pmiScore = Math.log(cognateSymbolPairProbability / randomSymbolPairProbability);
				globalCorr.setScore(symbolPairID, pmiScore);
				if (VERBOSE)
					System.err.println(" Global correspondence for " + symbolTable.toSymbolPair(symbolPairID) + " in iteration "
							+ iteration + ": " + Formatting.str3f(globalCorr.getScore(symbolPairID)) + "\t"
							+ Formatting.str3f(pmiScore) + "\t"
							+ "\t" + Formatting.str12f(cognateSymbolPairProbability) + " ("
							+ (int) cognatePairCorrespondenceDist.getObservationCount(symbolPairID) + "/"
							+ (int) cognatePairCorrespondenceDist.getObservationCountsSum() + ")\t"
							+ Formatting.str12f(randomSymbolPairProbability) + " ("
							+ (int) randomPairCorrespondenceDist.getObservationCount(symbolPairID) + "/"
							+ (int) randomPairCorrespondenceDist.getObservationCountsSum() + ")\t");
			}
			System.err.print(" done.\n");
		}

		return globalCorr;
	}
	
	public static CorrespondenceModel[][] inferLocalCorrespondenceModels(LexicalDatabase database,
			PhoneticSymbolTable symbolTable, int[] relevantLangIDs, CorrespondenceModel globalCorr) {
		CorrespondenceModel[][] localCorrModels = new CorrespondenceModel[database.getNumLanguages()][database.getNumLanguages()];
		for (int langID : relevantLangIDs) {
			System.err.println("Storing localCorrModels[" + langID + "][" + langID + "]");
			localCorrModels[langID][langID] = inferCorrModelForPair(database, symbolTable, langID, langID,
					globalCorr, globalCorr, globalCorr, null, null);
		}
		for (int lang1ID : relevantLangIDs) {
			for (int lang2ID : relevantLangIDs) {
				if (lang1ID == lang2ID) continue;
				System.err.println("Storing localCorrModels[" + lang1ID + "][" + lang2ID + "]");
				localCorrModels[lang1ID][lang2ID] = inferCorrModelForPair(database, symbolTable, lang1ID, lang2ID,
						globalCorr, localCorrModels[lang1ID][lang1ID], localCorrModels[lang2ID][lang2ID], null, null);
			}
		}
		return localCorrModels;
	}

	public static CorrespondenceModel[][] inferLocalCorrespondenceModels(LexicalDatabase database,
			PhoneticSymbolTable symbolTable, int[] relevantLangIDs, CorrespondenceModel globalCorr, InformationModel[] infoModels) {
		CorrespondenceModel[][] localCorrModels = new CorrespondenceModel[database.getNumLanguages()][database.getNumLanguages()];
		for (int langID : relevantLangIDs) {
			System.err.println("Storing localCorrModels[" + langID + "][" + langID + "]");
			localCorrModels[langID][langID] = inferCorrModelForPair(database, symbolTable, langID, langID,
					globalCorr, globalCorr, globalCorr, infoModels[langID], infoModels[langID]);
		}
		for (int lang1ID : relevantLangIDs) {
			for (int lang2ID : relevantLangIDs) {
				if (lang1ID == lang2ID) continue;
				System.err.println("Storing localCorrModels[" + lang1ID + "][" + lang2ID + "]");
				localCorrModels[lang1ID][lang2ID] = inferCorrModelForPair(database, symbolTable, lang1ID, lang2ID,
						globalCorr, localCorrModels[lang1ID][lang1ID], localCorrModels[lang2ID][lang2ID], infoModels[lang1ID], infoModels[lang2ID]);
			}
		}
		return localCorrModels;
	}
	
	public static CorrespondenceModel inferCorrModelForPair(LexicalDatabase database, PhoneticSymbolTable symbolTable,
			int lang1ID, int lang2ID, CorrespondenceModel globalCorr, CorrespondenceModel lang1SelfCorr,
			CorrespondenceModel lang2SelfCorr)
	{
		return inferCorrModelForPair(database, symbolTable, lang1ID, lang2ID, globalCorr, lang1SelfCorr, lang2SelfCorr);
	}

	public static CorrespondenceModel inferCorrModelForPair(LexicalDatabase database, PhoneticSymbolTable symbolTable,
			int lang1ID, int lang2ID, CorrespondenceModel globalCorr, CorrespondenceModel lang1SelfCorr,
			CorrespondenceModel lang2SelfCorr, InformationModel infoModel1, InformationModel infoModel2) {
		System.err.print("  Pair " + database.getLanguageCode(lang1ID) + "/" + database.getLanguageCode(lang2ID) + ":\n");

		CategoricalDistribution randomCorrespondenceDistForPair = new CategoricalDistribution(
				symbolTable.getSize() * symbolTable.getSize());
		System.err.print("    Step 1: " + NUM_RANDOM_PAIRS_LOCAL
				+ " random alignments to model the distribution in absence of correspondences ...");
		for (int i = 0; i < NUM_RANDOM_PAIRS_LOCAL; i++) {
			PhoneticString form1 = database.getRandomFormForLanguage(lang1ID);
			PhoneticString form2 = database.getRandomFormForLanguage(lang2ID);
			PhoneticStringAlignment alignment = NeedlemanWunschAlgorithm.constructAlignment(form1, form2, globalCorr,
					globalCorr, globalCorr, globalCorr);
			double[] infoScores = new double[alignment.getLength()];
			if (infoModel1 == null || infoModel2 == null) {
				Arrays.fill(infoScores, 1.0);
			}
			else {
				infoScores = InformationWeightedSequenceAlignment.combinedInfoScoresForAlignment(alignment, infoModel1, infoModel2);
			}
			for (int pos = 0; pos < alignment.getLength(); pos++) {
				randomCorrespondenceDistForPair.addObservation(alignment.getSymbolPairIDAtPos(pos, symbolTable), infoScores[pos]);
			}
		}
		System.err.print(" done.\n");

		// first iteration: use global sound correspondences
		CategoricalDistribution cognateCorrespondenceDistForPair = new CategoricalDistribution(
				symbolTable.getSize() * symbolTable.getSize());
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
							lang2Form, globalCorr, globalCorr, globalCorr, globalCorr);
					numPairs++;
					double[] infoScores = new double[alignment.getLength()];
					if (infoModel1 == null || infoModel2 == null) {
						Arrays.fill(infoScores, 1.0);
					}
					else {
						infoScores = InformationWeightedSequenceAlignment.combinedInfoScoresForAlignment(alignment, infoModel1, infoModel2);
					}
					if (alignment.normalizedDistanceScore <= COGNACY_CANDIDATE_WED_THRESHOLD) {
						for (int pos = 0; pos < alignment.getLength(); pos++) {
							cognateCorrespondenceDistForPair
									.addObservation(alignment.getSymbolPairIDAtPos(pos, symbolTable), infoScores[pos]);
						}
						numCognatePairs++;
					}
				}
			}
		}
		System.err.print(" done. Aligned " + numPairs + " form pairs, of which " + numCognatePairs
				+ " look like cognates (weighted edit distance <= " + COGNACY_CANDIDATE_WED_THRESHOLD + ")\n");

		System.err.print("          Comparing the distributions of symbol pairs to reestimate PMI scores ...");
		CorrespondenceModel localCorr = new CorrespondenceModel(symbolTable);
		for (int symbolPairID = 0; symbolPairID < symbolTable.getSize() * symbolTable.getSize(); symbolPairID++) {
			if (getMinUnigramCount(randomCorrespondenceDistForPair, symbolPairID, symbolTable.getSize()) == 0)
				continue;
			double cognateSymbolPairProbability = cognateCorrespondenceDistForPair.getProb(symbolPairID);
			double randomSymbolPairProbability = randomCorrespondenceDistForPair.getProb(symbolPairID);
			double pmiScore = Math.log(cognateSymbolPairProbability / randomSymbolPairProbability);
			double avgScore = LOCAL_PMI_RATIO * pmiScore + (1 - LOCAL_PMI_RATIO) * globalCorr.getScore(symbolPairID);
			localCorr.setScore(symbolPairID, avgScore);
		}
		System.err.print(" done.\n");

		System.err.print("    Step 3: Reestimation based on Needleman-Wunsch (" + NUM_LOCAL_CORR_REESTIMATIONS
				+ " iterations)\n");
		for (int iteration = 1; iteration <= NUM_LOCAL_CORR_REESTIMATIONS; iteration++) {
			System.err.print("    Iteration 0" + iteration + ": Finding WED-based cognate candidates ...");
			numPairs = 0;
			numCognatePairs = 0;
			cognateCorrespondenceDistForPair = new CategoricalDistribution(symbolTable.getSize() * symbolTable.getSize());
			for (int conceptID = 0; conceptID < database.getNumConcepts(); conceptID++) {
				List<List<Integer>> formsPerLang = database.getFormIDsForConceptPerLanguage(conceptID);
				for (int lang1FormID : formsPerLang.get(lang1ID)) {
					PhoneticString lang1Form = database.getForm(lang1FormID);
					for (int lang2FormID : formsPerLang.get(lang2ID)) {
						PhoneticString lang2Form = database.getForm(lang2FormID);
						PhoneticStringAlignment alignment = NeedlemanWunschAlgorithm.constructAlignment(lang1Form,
								lang2Form, globalCorr, localCorr, lang1SelfCorr, lang2SelfCorr);
						numPairs++;
						double[] infoScores = new double[alignment.getLength()];
						if (infoModel1 == null || infoModel2 == null) {
							Arrays.fill(infoScores, 1.0);
						}
						else {
							infoScores = InformationWeightedSequenceAlignment.combinedInfoScoresForAlignment(alignment, infoModel1, infoModel2);
						}
						if (alignment.normalizedDistanceScore <= COGNACY_CANDIDATE_WED_THRESHOLD) {
							for (int pos = 0; pos < alignment.getLength(); pos++) {
								cognateCorrespondenceDistForPair
										.addObservation(alignment.getSymbolPairIDAtPos(pos, symbolTable), infoScores[pos]);
							}
							numCognatePairs++;
						}
					}
				}
			}

			System.err
					.print(" done. " + numCognatePairs + " form pairs look like cognates (normalized distance score <= "
							+ COGNACY_CANDIDATE_WED_THRESHOLD + ")\n");

			System.err.print("          Comparing the distributions of symbol pairs to reestimate PMI scores ...");
			localCorr = new CorrespondenceModel(symbolTable);
			for (int symbolPairID = 0; symbolPairID < symbolTable.getSize() * symbolTable.getSize(); symbolPairID++) {
				if (getMinUnigramCount(randomCorrespondenceDistForPair, symbolPairID, symbolTable.getSize()) == 0)
					continue;
				double pmiScore = 0.0;
				double cij = cognateCorrespondenceDistForPair.getObservationCount(symbolPairID);
				double cognateSymbolPairProbability = (cij
						/ cognateCorrespondenceDistForPair.getObservationCountsSum());
				double randomSymbolPairProbability = (randomCorrespondenceDistForPair.getObservationCount(symbolPairID)
						/ randomCorrespondenceDistForPair.getObservationCountsSum());
				if (cij > 0 && randomCorrespondenceDistForPair.getObservationCount(symbolPairID) > 0) {
					pmiScore = Math.log(cognateSymbolPairProbability / randomSymbolPairProbability);
				}
				double avgScore = LOCAL_PMI_RATIO * pmiScore + (1 - LOCAL_PMI_RATIO) * globalCorr.getScore(symbolPairID);
				//for memory capacity, do not store scores that are close to zero
				if (Math.abs(avgScore) > 0.1) {
					localCorr.setScore(symbolPairID, avgScore);
					if (VERBOSE)
						System.err.println(database.getLanguageCode(lang1ID) + "-" + database.getLanguageCode(lang2ID)
								+ " correspondence for " + symbolTable.toSymbolPair(symbolPairID) + " in iteration "
								+ iteration + ": " + Formatting.str3f(globalCorr.getScore(symbolPairID)) + "\t"
								+ Formatting.str3f(pmiScore) + "\t" + Formatting.str3f(localCorr.getScore(symbolPairID))
								+ "\t" + Formatting.str3f(cognateSymbolPairProbability) + " ("
								+ (int) cognateCorrespondenceDistForPair.getObservationCount(symbolPairID) + "/"
								+ (int) cognateCorrespondenceDistForPair.getObservationCountsSum() + ")\t"
								+ Formatting.str3f(randomSymbolPairProbability) + " ("
								+ (int) randomCorrespondenceDistForPair.getObservationCount(symbolPairID) + "/"
								+ (int) randomCorrespondenceDistForPair.getObservationCountsSum() + ")\t");
				}
			}
			System.err.print(" done.\n");

			if (VERBOSE)
				ConceptLevelWeightedEditDistanceOutput.distanceOutput(database, symbolTable, lang1ID, lang2ID,
						globalCorr, localCorr);
		}
		return localCorr;
	}
	
	public static double getMinUnigramCount(CategoricalDistribution dist, int bigramID, int symbolTableSize)
	{
		int i = bigramID / symbolTableSize;
		int j = bigramID % symbolTableSize;
		double iCount = 0;
		for (int k = 0; k < symbolTableSize; k++)
		{
			iCount += dist.getObservationCount(i * symbolTableSize + k);
		}
		double jCount = 0;
		for (int k = 0; k < symbolTableSize; k++)
		{
			jCount += dist.getObservationCount(k * symbolTableSize + j);
		}
		return Math.min(iCount, jCount);
	}

}
