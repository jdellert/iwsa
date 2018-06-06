package de.jdellert.iwsa;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Map;
import java.util.TreeMap;

import de.jdellert.iwsa.corrmodel.CorrespondenceModel;
import de.jdellert.iwsa.corrmodel.CorrespondenceModelInference;
import de.jdellert.iwsa.corrmodel.CorrespondenceModelStorage;
import de.jdellert.iwsa.data.CLDFImport;
import de.jdellert.iwsa.data.LexicalDatabase;
import de.jdellert.iwsa.infomodel.InformationModel;
import de.jdellert.iwsa.infomodel.InformationModelInference;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;

public class CognateClusteringIWDSC {
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

			CorrespondenceModel[][] localCorrModels = null;
				try {
					System.err.print("Attempting to load existing local correspondence models from " + args[0]
							+ "-local-iw.corr ... ");
					localCorrModels = CorrespondenceModelStorage.loadCorrespondenceModels(
							new ObjectInputStream(new FileInputStream(args[0] + "-local-iw.corr")), relevantLangToID);
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
					localCorrModels = CorrespondenceModelInference.inferLocalCorrespondenceModels(database, symbolTable,
							relevantLangIDs, globalCorrModel, infoModels);
					CorrespondenceModelStorage.writeLocalModelsToFile(localCorrModels, database.getLanguageCodes(),
							symbolTable, args[0] + "-local-iw.corr");
				}
				
			for (int conceptID = 0; conceptID < database.getNumConcepts(); conceptID++)
			{
				//TODO: build distance matrix
					
				
				//TODO: clustering
				
					
				//TODO: store cluster IDs in database
				
			}
			//TODO: CLDF output
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
