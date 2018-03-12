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
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;

public class CorrespondenceModelOutput {
	public static void main(String[] args) {
		if (args.length != 1 && args.length != 3) {
			System.err.println("Usage: CorrespondenceModelOutput [globalCorrespondenceFile]");
			System.err.println("       CorrespondenceModelOutput [localCorrespondenceFile] [langCode1] [langCode2]");
		}
		if (args.length == 1) {
			CorrespondenceModel globalCorrModel = null;
			try {
				globalCorrModel = CorrespondenceModelStorage
						.loadCorrespondenceModel(new ObjectInputStream(new FileInputStream(args[0])));

				PhoneticSymbolTable symbolTable = globalCorrModel.getSymbolTable();
				for (int symbol1ID = 0; symbol1ID < symbolTable.getSize(); symbol1ID++) {
					for (int symbol2ID = 0; symbol2ID < symbolTable.getSize(); symbol2ID++) {
						System.out.print(symbolTable.toSymbol(symbol1ID) + "\t");
						System.out.print(symbolTable.toSymbol(symbol2ID) + "\t");
						System.out.print(globalCorrModel.getScore(symbol1ID, symbol2ID) + "\n");
					}
				}

			} catch (FileNotFoundException e) {
				System.err.print(" file not found, need to infer global model first.\n");
			} catch (IOException e) {
				System.err.print(" format error, need to reinfer global model.\n");
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				System.exit(0);
			}

		}
		// extract local correspondence model for a language pair
		if (args.length == 3) {
			String lang1 = args[1];
			String lang2 = args[2];
			Map<String, Integer> relevantLangToID = new TreeMap<String, Integer>();
			relevantLangToID.put(lang1, 0);
			relevantLangToID.put(lang2, 1);
			CorrespondenceModel[][] localCorrModels = null;
			try {
				System.err.print(
						"Attempting to load existing local correspondence model from " + args[0] + "-local.corr ... ");
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

			PhoneticSymbolTable symbolTable = localCorrModels[0][1].getSymbolTable();
			for (int symbol1ID = 0; symbol1ID < symbolTable.getSize(); symbol1ID++) {
				for (int symbol2ID = 0; symbol2ID < symbolTable.getSize(); symbol2ID++) {
					double score = localCorrModels[0][1].getScore(symbol1ID, symbol2ID);
					if (score != 0.0) {
						System.out.print(symbolTable.toSymbol(symbol1ID) + "\t");
						System.out.print(symbolTable.toSymbol(symbol2ID) + "\t");
						System.out.print(score + "\n");
					}
				}
			}
		}
	}
}
