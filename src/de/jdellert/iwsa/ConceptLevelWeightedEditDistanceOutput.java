package de.jdellert.iwsa;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import de.jdellert.iwsa.align.NeedlemanWunschAlgorithm;
import de.jdellert.iwsa.align.PhoneticStringAlignment;
import de.jdellert.iwsa.align.PhoneticStringAlignmentOutput;
import de.jdellert.iwsa.corrmodel.CorrespondenceModel;
import de.jdellert.iwsa.corrmodel.CorrespondenceModelInference;
import de.jdellert.iwsa.corrmodel.CorrespondenceModelStorage;
import de.jdellert.iwsa.data.CLDFImport;
import de.jdellert.iwsa.data.LexicalDatabase;
import de.jdellert.iwsa.infomodel.InformationModel;
import de.jdellert.iwsa.infomodel.InformationModelInference;
import de.jdellert.iwsa.sequence.PhoneticString;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;

public class ConceptLevelWeightedEditDistanceOutput {
	public static final boolean ALIGNMENT_OUTPUT = false;
	
	public static void main(String[] args) {
		try {
			LexicalDatabase database = CLDFImport.loadDatabase(args[0], true);
			PhoneticSymbolTable symbolTable = database.getSymbolTable();

			// default: assume all languages are relevant, and part of the inference
			String[] relevantLangCodes = database.getLanguageCodes();
			int[] relevantLangIDs = new int[database.getNumLanguages()];
			for (int i = 0; i < relevantLangIDs.length; i++) {
				relevantLangIDs[i] = i;
			}

			// interpret additional arguments as language IDs
			if (args.length > 1) {
				relevantLangCodes = new String[args.length - 1];
				relevantLangIDs = new int[args.length - 1];
				for (int i = 1; i < args.length; i++) {
					int langID = database.getIDForLanguageCode(args[i]);
					if (langID == -1) {
						System.err.println("ERROR: language code " + args[i] + " does not occur in database!");
						System.exit(1);
					}
					relevantLangCodes[i - 1] = args[i];
					relevantLangIDs[i - 1] = langID;
				}
			}
			
			Map<String,Integer> relevantLangToID = new TreeMap<String, Integer>();
			for (int langID = 0; langID < relevantLangIDs.length; langID++)
			{
				relevantLangToID.put(relevantLangCodes[langID], relevantLangIDs[langID]);
			}

			CorrespondenceModel globalCorrModel = null;
			try {
				System.err.print("Attempting to load existing global correspondence model from " + args[0]
						+ "-global-nw.corr ... ");
				globalCorrModel = CorrespondenceModelStorage
						.loadCorrespondenceModel(new ObjectInputStream(new FileInputStream(args[0] + "-global-nw.corr")));
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
				globalCorrModel = CorrespondenceModelInference.inferGlobalCorrespondenceModel(database, symbolTable);
				CorrespondenceModelStorage.writeGlobalModelToFile(globalCorrModel, args[0] + "-global-nw.corr");

			}

			CorrespondenceModel[][] localCorrModels = null;
			try {
				System.err.print(
						"Attempting to load existing global correspondence model from " + args[0] + "-local-nw.corr ... ");
				localCorrModels = CorrespondenceModelStorage
						.loadCorrespondenceModels(new ObjectInputStream(new FileInputStream(args[0] + "-local-nw.corr")), relevantLangToID);
				System.err.print(
						"done.\nStage 2: Pairwise sound correspondences - skipped because previously inferred models were found. Delete model file and rerun to cause re-inference.\n");

			} catch (FileNotFoundException e) {
				System.err.print(" file not found, need to infer pairwise correspondence models first.\n");
			} catch (IOException e) {
				System.err.print(" format error, need to reinfer pairwise correspondence models.\n");
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				System.exit(0);
			}
			if (localCorrModels == null) {
				System.err.print("Stage 2: Inference of sound correspondence matrices for each language pair\n");
				localCorrModels = CorrespondenceModelInference.inferLocalCorrespondenceModels(database, symbolTable, relevantLangIDs, globalCorrModel);
				CorrespondenceModelStorage.writeLocalModelsToFile(localCorrModels, database.getLanguageCodes(), symbolTable,
						args[0] + "-local-nw.corr");
			}

			// finally: output of distances
			distanceOutput(database, symbolTable, relevantLangIDs, globalCorrModel, localCorrModels);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void distanceOutput(LexicalDatabase database, PhoneticSymbolTable symbolTable, int[] relevantLangIDs,
			CorrespondenceModel globalCorrModel, CorrespondenceModel[][] localCorrModels) {
		for (int conceptID = 0; conceptID < database.getNumConcepts(); conceptID++) {
			List<List<Integer>> formsPerLang = database.getFormIDsForConceptPerLanguage(conceptID);
			for (int lang1ID : relevantLangIDs) {
				for (int lang2ID : relevantLangIDs) {
					for (int lang1FormID : formsPerLang.get(lang1ID)) {
						PhoneticString lang1Form = database.getForm(lang1FormID);
						for (int lang2FormID : formsPerLang.get(lang2ID)) {
							PhoneticString lang2Form = database.getForm(lang2FormID);
							PhoneticStringAlignment globalWeightsAlignment = NeedlemanWunschAlgorithm
									.constructAlignment(lang1Form, lang2Form, globalCorrModel, globalCorrModel, globalCorrModel, globalCorrModel);
							double globalWeightDistance = globalWeightsAlignment.normalizedDistanceScore;
							System.out.println(PhoneticStringAlignmentOutput.needlemanWunschtoString(globalWeightsAlignment, symbolTable, globalCorrModel, globalCorrModel, globalCorrModel, globalCorrModel));
							PhoneticStringAlignment localWeightsAlignment = NeedlemanWunschAlgorithm
									.constructAlignment(lang1Form, lang2Form, globalCorrModel, localCorrModels[lang1ID][lang2ID], localCorrModels[lang1ID][lang1ID], localCorrModels[lang2ID][lang2ID]);
							double localWeightDistance = localWeightsAlignment.normalizedDistanceScore;
							double minDistance = Math.min(globalWeightDistance, localWeightDistance);
							System.out.println(PhoneticStringAlignmentOutput.needlemanWunschtoString(localWeightsAlignment, symbolTable, globalCorrModel, localCorrModels[lang1ID][lang2ID], localCorrModels[lang1ID][lang1ID], localCorrModels[lang2ID][lang2ID]));
							System.out.print(database.getConceptName(conceptID) + "\t");
							System.out.print(database.getLanguageCode(lang1ID) + "\t"
									+ database.getLanguageCode(lang2ID) + "\t");
							System.out.print(database.getAnnotation("Word_Form", lang1FormID) + "\t"
									+ database.getAnnotation("Word_Form", lang2FormID) + "\t");
							System.out.print(
									lang1Form.toString(symbolTable) + "\t" + lang2Form.toString(symbolTable) + "\t");
							System.out.println(globalWeightDistance + "\t" + localWeightDistance + "\t" + minDistance);
							System.out.println();
						}
					}
				}
			}
		}
	}

	public static void distanceOutput(LexicalDatabase database, PhoneticSymbolTable symbolTable, int lang1ID,
			int lang2ID, CorrespondenceModel globalCorrModel, CorrespondenceModel localCorrModel) {
		for (int conceptID = 0; conceptID < database.getNumConcepts(); conceptID++) {
			List<List<Integer>> formsPerLang = database.getFormIDsForConceptPerLanguage(conceptID);
			for (int lang1FormID : formsPerLang.get(lang1ID)) {
				PhoneticString lang1Form = database.getForm(lang1FormID);
				for (int lang2FormID : formsPerLang.get(lang2ID)) {
					PhoneticString lang2Form = database.getForm(lang2FormID);
					PhoneticStringAlignment globalWeightsAlignment = NeedlemanWunschAlgorithm
							.constructAlignment(lang1Form, lang2Form, globalCorrModel, globalCorrModel, globalCorrModel, globalCorrModel);
					double globalWeightDistance = globalWeightsAlignment.normalizedDistanceScore;
					PhoneticStringAlignment localWeightsAlignment = NeedlemanWunschAlgorithm
							.constructAlignment(lang1Form, lang2Form, globalCorrModel, localCorrModel, globalCorrModel, globalCorrModel);
					double localWeightDistance = localWeightsAlignment.normalizedDistanceScore;
					double minDistance = Math.min(globalWeightDistance, localWeightDistance);
					System.out.print(database.getConceptName(conceptID) + "\t");
					System.out
							.print(database.getLanguageCode(lang1ID) + "\t" + database.getLanguageCode(lang2ID) + "\t");
					System.out.print(database.getAnnotation("Word_Form", lang1FormID) + "\t"
							+ database.getAnnotation("Word_Form", lang2FormID) + "\t");
					System.out.print(lang1Form.toString(symbolTable) + "\t" + lang2Form.toString(symbolTable) + "\t");
					System.out.println(globalWeightDistance + "\t" + localWeightDistance + "\t" + minDistance);
				}
			}
		}
	}
}
