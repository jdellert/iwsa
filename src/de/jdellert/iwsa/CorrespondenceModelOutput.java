package de.jdellert.iwsa;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;

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
		if (args.length == 3) {
			//TODO: extract local correspondence model for a language pair
		}
	}
}
