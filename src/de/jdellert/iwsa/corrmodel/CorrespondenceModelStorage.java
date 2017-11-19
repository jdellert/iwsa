package de.jdellert.iwsa.corrmodel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.TreeMap;

import de.jdellert.iwsa.sequence.PhoneticSymbolTable;

public class CorrespondenceModelStorage {

	public static void writeGlobalModelToFile(CorrespondenceModel globalCorrModel, String fileName)
			throws FileNotFoundException, IOException {
		System.err.print("Writing global correspondence model to " + fileName + " ...");
		ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(new File(fileName)));
		outputStream.writeObject(globalCorrModel.symbolTable);
		outputStream.writeObject(globalCorrModel.scores);
		outputStream.close();
		System.err.println("done.");
	}

	public static void writeLocalModelsToFile(CorrespondenceModel[][] localCorrModels, String[] langIDs,
			PhoneticSymbolTable symbolTable, String fileName) throws FileNotFoundException, IOException {
		System.err.print("Writing pair-specific correspondence models to " + fileName);
		ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(new File(fileName)));
		outputStream.writeObject(langIDs);
		outputStream.writeObject(symbolTable);
		for (int i = 0; i < langIDs.length; i++) {
			for (int j = 0; j < langIDs.length; j++) {
				if (localCorrModels[i][j] == null) {
					outputStream.writeObject(new TreeMap<Integer, Double>());
				} else {
					outputStream.writeObject(localCorrModels[i][j].scores);
				}
			}
		}
		outputStream.close();
		System.err.println("done.");
	}

	public static CorrespondenceModel loadCorrespondenceModel(ObjectInputStream in)
			throws IOException, ClassNotFoundException {
		PhoneticSymbolTable symbolTable = (PhoneticSymbolTable) in.readObject();
		Map<Integer, Double> scores = (Map<Integer, Double>) in.readObject();
		CorrespondenceModel correspondenceModel = new CorrespondenceModel(symbolTable);
		correspondenceModel.scores = scores;
		return correspondenceModel;
	}

	public static CorrespondenceModel[][] loadCorrespondenceModels(ObjectInputStream in)
			throws IOException, ClassNotFoundException {
		String langIDs[] = (String[]) in.readObject();
		PhoneticSymbolTable symbolTable = (PhoneticSymbolTable) in.readObject();
		CorrespondenceModel[][] correspondenceModels = new CorrespondenceModel[langIDs.length][langIDs.length];
		for (int i = 0; i < langIDs.length; i++) {
			for (int j = 0; j < langIDs.length; j++) {
				CorrespondenceModel correspondenceModel = new CorrespondenceModel(symbolTable);
				correspondenceModel.scores = (Map<Integer, Double>) in.readObject();
				correspondenceModels[i][j] = correspondenceModel;
			}
		}
		return correspondenceModels;
	}
}
