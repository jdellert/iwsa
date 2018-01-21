package de.jdellert.iwsa.eval;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import de.jdellert.iwsa.align.InformationWeightedSequenceAlignment;
import de.jdellert.iwsa.align.LevenshteinAlignmentAlgorithm;
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
import de.jdellert.iwsa.util.io.Formatting;
import de.jdellert.iwsa.util.io.SimpleFormatReader;
import de.jdellert.iwsa.util.io.StringUtils;

public class AllDistancesForWordPairFile {
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

			Map<String, Integer> relevantLangToID = new TreeMap<String, Integer>();
			for (int langID = 0; langID < relevantLangIDs.length; langID++) {
				relevantLangToID.put(relevantLangCodes[langID], relevantLangIDs[langID]);
			}

			CorrespondenceModel globalCorrModel = null;
			try {
				System.err.print("Attempting to load existing global correspondence model from " + args[0]
						+ "-global.corr ... ");
				globalCorrModel = CorrespondenceModelStorage
						.loadCorrespondenceModel(new ObjectInputStream(new FileInputStream(args[0] + "-global.corr")));
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
				System.err.println("No global PMI scores found! Run global PMI scores inference to create " + args[0]
						+ "-global.corr\n");
				System.exit(1);
			}

			CorrespondenceModel[][] localCorrModels = null;
			try {
				System.err.print(
						"Attempting to load existing local correspondence models from " + args[0] + "-local.corr ... ");
				localCorrModels = CorrespondenceModelStorage.loadCorrespondenceModels(
						new ObjectInputStream(new FileInputStream(args[0] + "-local.corr")), relevantLangToID);
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
			if (globalCorrModel == null) {
				System.err.println("No local PMI scores found! Run local PMI scores inference to create " + args[0]
						+ "-local.corr\n");
				System.exit(1);
			}

			try {
				// load the gold standard pairs
				String goldStandardFileName = args[1];
				
				List<String[]> lines = SimpleFormatReader.arrayFromTSV(goldStandardFileName);
				
		        System.out.println("concept\tlang1\torth1\tpron1\tid1\tlang2\torth2\tpron2\tid2\tcognacy\tED\tlocalWED\tglobalWED\tcombinedWED\tlocalIWED\tglobalIWED\tcombinedIWED");
		        
		        for (String[] line : lines)
		        {
		        	String concept = line[0];
		        	String lang1 = line[1];
		        	String orth1 = line[2];
		        	String lang2 = line[5];
		        	String orth2 = line[6];
		        	
		        	int lang1ID = database.getIDForLanguageCode(lang1);
		        	int lang2ID = database.getIDForLanguageCode(lang2);
		        	if (lang1ID == -1 || lang2ID == -1)
		        	{
		        		//print line with question marks
		        		System.out.print(StringUtils.join("\t", line));
		        		System.out.println(StringUtils.multiply("\t?", 7));
		        		continue;
		        	}
		        	
					PhoneticString lang1Form = database.getFormForLangConceptOrth(lang1, concept, orth1);
					PhoneticString lang2Form = database.getFormForLangConceptOrth(lang2, concept, orth2);
					
		        	if (lang1Form == null || lang2Form == null)
		        	{
		        		//print line with question marks
		        		System.out.print(StringUtils.join("\t", line));
		        		System.out.println(StringUtils.multiply("\t?", 7));
		        		continue;
		        	}
		        	
		        	double editDistance = LevenshteinAlignmentAlgorithm.computeNormalizedEditDistance(lang1Form, lang2Form);
		        	
		        	
					PhoneticStringAlignment globalWeightsAlignment = NeedlemanWunschAlgorithm
							.constructAlignment(lang1Form, lang2Form, globalCorrModel, globalCorrModel, globalCorrModel, globalCorrModel);
					double globalWED = globalWeightsAlignment.normalizedDistanceScore;
					System.out.println(PhoneticStringAlignmentOutput.needlemanWunschtoString(globalWeightsAlignment, symbolTable, globalCorrModel, globalCorrModel, globalCorrModel, globalCorrModel));
					PhoneticStringAlignment localWeightsAlignment = NeedlemanWunschAlgorithm
							.constructAlignment(lang1Form, lang2Form, globalCorrModel, localCorrModels[lang1ID][lang2ID], localCorrModels[lang1ID][lang1ID], localCorrModels[lang2ID][lang2ID]);
					System.out.println(PhoneticStringAlignmentOutput.needlemanWunschtoString(localWeightsAlignment, symbolTable, globalCorrModel, localCorrModels[lang1ID][lang2ID], localCorrModels[lang1ID][lang1ID], localCorrModels[lang2ID][lang2ID]));
					double localWED = localWeightsAlignment.normalizedDistanceScore;
					double combinedWED = Math.min(globalWED, localWED);
					
					PhoneticStringAlignment globalInfoWeightsAlignment = InformationWeightedSequenceAlignment
							.constructAlignment(lang1Form, lang2Form, globalCorrModel, globalCorrModel, globalCorrModel, globalCorrModel, infoModels[lang1ID], infoModels[lang2ID]);
					double globalIWED = globalInfoWeightsAlignment.normalizedDistanceScore;
					PhoneticStringAlignment localInfoWeightsAlignment = InformationWeightedSequenceAlignment
							.constructAlignment(lang1Form, lang2Form, globalCorrModel, localCorrModels[lang1ID][lang2ID], localCorrModels[lang1ID][lang1ID], localCorrModels[lang2ID][lang2ID], infoModels[lang1ID], infoModels[lang2ID]);
					double localIWED = localInfoWeightsAlignment.normalizedDistanceScore;
					double combinedIWED = Math.min(globalIWED, localIWED);
		        	
					System.out.print(StringUtils.join("\t", line));
					
					System.out.print("\t" + Formatting.str6f(editDistance));
					
					System.out.print("\t" + Formatting.str6f(localWED));
					System.out.print("\t" + Formatting.str6f(globalWED));
					System.out.print("\t" + Formatting.str6f(combinedWED));
					
					System.out.print("\t" + Formatting.str6f(localIWED));
					System.out.print("\t" + Formatting.str6f(globalIWED));
					System.out.print("\t" + Formatting.str6f(combinedIWED));

					System.out.println();
		        }
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
