package de.jdellert.iwsa.bootstrap;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import de.jdellert.iwsa.CognateClusteringIWDSC;
import de.jdellert.iwsa.align.InformationWeightedSequenceAlignment;
import de.jdellert.iwsa.align.PhoneticStringAlignment;
import de.jdellert.iwsa.cluster.FlatClustering;
import de.jdellert.iwsa.corrmodel.CorrespondenceModel;
import de.jdellert.iwsa.corrmodel.CorrespondenceModelInference;
import de.jdellert.iwsa.corrmodel.CorrespondenceModelStorage;
import de.jdellert.iwsa.data.CLDFImport;
import de.jdellert.iwsa.data.LexicalDatabase;
import de.jdellert.iwsa.infomodel.InformationModel;
import de.jdellert.iwsa.infomodel.InformationModelInference;
import de.jdellert.iwsa.sequence.PhoneticString;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;
import de.jdellert.iwsa.util.io.SimpleFormatReader;

public class CognateOverlapAvgFormDistBootstrap {
	public static final NumberFormat formatter = new DecimalFormat("0.0000").getInstance(Locale.ENGLISH);
	
	public static void main(String[] args) {
		try {
			LexicalDatabase database = CLDFImport.loadDatabase(args[0], true);
			PhoneticSymbolTable symbolTable = database.getSymbolTable();
	
			InformationModel[] infoModels = InformationModelInference.inferInformationModels(database, symbolTable);
	
			// default: assume all languages are relevant, and part of the inference
			String[] relevantLangCodes = database.getLanguageCodes();
			int[] relevantLangIDs = new int[database.getNumLanguages()];
			for (int i = 0; i < relevantLangIDs.length; i++) {
				relevantLangIDs[i] = i;
			}
	
			// interpret additional argument as a list of language IDs
			if (args.length > 1) {
				relevantLangCodes = SimpleFormatReader.listFromFile(args[1]).stream().toArray(String[]::new);
				relevantLangIDs = new int[relevantLangCodes.length];
				
				for (int i = 0; i < relevantLangCodes.length; i++) {
					int langID = database.getIDForLanguageCode(relevantLangCodes[i]);
					if (langID == -1) {
						System.err.println("ERROR: language code " + relevantLangCodes[i] + " does not occur in database!");
						System.exit(1);
					}
					relevantLangIDs[i] = langID;
				}
			}
	
			Map<String, Integer> relevantLangToID = new TreeMap<String, Integer>();
			for (int langID = 0; langID < relevantLangIDs.length; langID++) {
				relevantLangToID.put(relevantLangCodes[langID], relevantLangIDs[langID]);
			}
	
			CorrespondenceModel globalCorrModel = null;
			try {
				System.err.print("Attempting to load existing global correspondence model from " + args[0]
						+ "-global-iw.corr ... ");
				globalCorrModel = CorrespondenceModelStorage
						.loadCorrespondenceModel(new ObjectInputStream(new FileInputStream(args[0] + "-global-iw.corr")));
				System.err.print(
						"done.\nStage 1: Global sound correspondences - skipped because previously inferred model was found. Delete model file and rerun to cause re-inference.\n");
			} catch (FileNotFoundException e) {
				System.err.print(" file not found, need to infer global model first.\n");
			} catch (IOException e) {
				System.err.print(" format error, need to reinfer global model.\n");
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				System.exit(0);
			}
			if (globalCorrModel == null) {
				System.err.print("Stage 1: Inference of global PMI scores\n");
				globalCorrModel = CorrespondenceModelInference.inferGlobalCorrespondenceModel(database, symbolTable, infoModels);
				CorrespondenceModelStorage.writeGlobalModelToFile(globalCorrModel, args[0] + "-global-iw.corr");
			}
			
			produceMatricesForSample(args[0] + "matrices-original", database, relevantLangIDs, globalCorrModel, infoModels);
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void produceMatricesForSample(String fileName, LexicalDatabase database, int[] relevantLangIDs, CorrespondenceModel globalCorrModel, InformationModel[] infoModels) {
		//infer local correspondences for all necessary language pairs
		System.err.print(fileName + ", stage 1: Inference of sound correspondence matrices for each language pair\n");
		CorrespondenceModel localCorrModels[][] = CorrespondenceModelInference.inferLocalCorrespondenceModels(database, database.getSymbolTable(), relevantLangIDs, globalCorrModel, infoModels);
		
		List<Map<Integer,Integer>> formsToCognateSetPerConcept = new ArrayList<Map<Integer,Integer>>(database.getNumConcepts());
		List<Double[][]> formDistances = new ArrayList<Double[][]>(database.getNumConcepts());
		
		aggregateAnalysis(formsToCognateSetPerConcept, formDistances, database, relevantLangIDs, globalCorrModel, localCorrModels, infoModels);
		
		for (int i = 0; i < relevantLangIDs.length; i++) {
			int lang1ID = relevantLangIDs[i];
			for (int j = i; j < relevantLangIDs.length; j++) {
				int lang2ID = relevantLangIDs[j];
				
				double cogOverlap = computeCognateOverlap(formsToCognateSetPerConcept, lang1ID, lang2ID, database);
				double avgFrmDist = computeAverageFormDistance(formDistances, i, j);
				System.err.println(database.getLanguageCode(lang1ID) + "\t" + database.getLanguageCode(lang2ID) + "\t" + formatter.format(cogOverlap) + "\t" + formatter.format(avgFrmDist));
			}
		}
	}
	
	public static void aggregateAnalysis(List<Map<Integer,Integer>> formsToCognateSetPerConcept, List<Double[][]> formDistances, LexicalDatabase database, int[] relevantLangIDs, CorrespondenceModel globalCorrModel, CorrespondenceModel localCorrModels[][], InformationModel[] infoModels) {
		for (int conceptID = 0; conceptID < database.getNumConcepts(); conceptID++) {
			System.err.println("Clustering words for concept #" + conceptID + " (" + database.getConceptName(conceptID) + ")");
			
			// build distance matrix
			List<Integer> formIDs = database.getFormIDsForConcept(conceptID);
			Map<Integer, Integer> formIDToIndex = new TreeMap<Integer, Integer>();
			for (int index = 0; index < formIDs.size(); index++) {
				formIDToIndex.put(formIDs.get(index), index);
			}

			double[][] distanceMatrix = new double[formIDs.size()][formIDs.size()];
			Double[][] minDistanceMatrix = new Double[relevantLangIDs.length][relevantLangIDs.length];
			for (Double[] row : minDistanceMatrix) {
				Arrays.fill(row, Double.POSITIVE_INFINITY);
			}

			List<List<Integer>> formsPerLang = database.getFormIDsForConceptPerLanguage(conceptID);
			for (int i = 0; i < relevantLangIDs.length; i++)
			{
				int lang1ID = relevantLangIDs[i];
				for (int j = i; j < relevantLangIDs.length; j++) {
					int lang2ID = relevantLangIDs[j];
					for (int lang1FormID : formsPerLang.get(lang1ID)) {
						int index1 = formIDToIndex.get(lang1FormID);
						PhoneticString lang1Form = database.getForm(lang1FormID);
						for (int lang2FormID : formsPerLang.get(lang2ID)) {
							int index2 = formIDToIndex.get(lang2FormID);
							PhoneticString lang2Form = database.getForm(lang2FormID);
							PhoneticStringAlignment localWeightsAlignment = InformationWeightedSequenceAlignment
									.constructAlignment(lang1Form, lang2Form, globalCorrModel,
											localCorrModels[lang1ID][lang2ID], localCorrModels[lang1ID][lang1ID],
											localCorrModels[lang2ID][lang2ID], infoModels[lang1ID],
											infoModels[lang2ID]);
							double localWeightDistance = localWeightsAlignment.normalizedDistanceScore;
							if (localWeightDistance < 0.0) localWeightDistance = 0.0;
							localWeightDistance *= localWeightDistance;
							if (localWeightDistance > CognateClusteringIWDSC.MAX_DIST_VAL) localWeightDistance = CognateClusteringIWDSC.MAX_DIST_VAL;
							localWeightDistance /= CognateClusteringIWDSC.MAX_DIST_VAL;
							if (index1 == index2) localWeightDistance = 0.0;
							distanceMatrix[index1][index2] = localWeightDistance;
							distanceMatrix[index2][index1] = localWeightDistance;
							if (localWeightDistance < minDistanceMatrix[i][j]) {
								minDistanceMatrix[i][j] = localWeightDistance;
								minDistanceMatrix[j][i] = localWeightDistance;
							}
						}
					}
				}
			}
			
			formDistances.add(minDistanceMatrix);

			// UPGMA clustering
			Set<Set<Integer>> cognateSets = FlatClustering.upgma(distanceMatrix, CognateClusteringIWDSC.THRESHOLD);
			
			Map<Integer,Integer> formsToCognateSet = new TreeMap<Integer,Integer>();
			int cogSetID = 0;
			for (Set<Integer> cognateSet : cognateSets) {
				for (Integer id : cognateSet) {
					formsToCognateSet.put(formIDs.get(id), cogSetID);
				}
				cogSetID++;
			}
			
			formsToCognateSetPerConcept.add(formsToCognateSet);
			
		}
	}
	
	public static double computeCognateOverlap(List<Map<Integer,Integer>> formsToCognateSetPerConcept, int lang1ID, int lang2ID, LexicalDatabase database) {
		double lang1FormNum = 0.0;
		double lang2FormNum = 0.0;
		double lang1lang2SharedSets = 0.0;
		for (int conceptID = 0; conceptID < database.getNumConcepts(); conceptID++) {
			Map<Integer,Integer> formToCognateSetID = formsToCognateSetPerConcept.get(conceptID);
			List<Integer> lang1FormIDs = database.getFormIDsForLanguageAndConcept(lang1ID, conceptID);
			List<Integer> lang2FormIDs = database.getFormIDsForLanguageAndConcept(lang2ID, conceptID);
			lang1FormNum += lang1FormIDs.size();
			lang2FormNum += lang2FormIDs.size();
			for (int lang1FormID : lang1FormIDs) {
				int cogSetID = formToCognateSetID.get(lang1FormID);
				boolean foundCognate = false;
				for (int lang2FormID : lang2FormIDs) {
					if (formToCognateSetID.get(lang2FormID) == cogSetID) {
						foundCognate = true;
						break;
					}
				}
				if (foundCognate) {
					lang1lang2SharedSets += 1.0;
				}
			}
		}
		return lang1lang2SharedSets / Math.max(lang1FormNum, lang2FormNum);
	}
	
	public static double computeAverageFormDistance(List<Double[][]> formDistances, int i, int j) {
		double sum = 0;
		int numEntries = 0;
		for (int k = 0; k < formDistances.size(); k++) {
			double val = formDistances.get(k)[i][j];
			if (val < Double.POSITIVE_INFINITY) {
				sum += val;
				numEntries += 1;
			}
		}
		return sum/numEntries;
	}
}
