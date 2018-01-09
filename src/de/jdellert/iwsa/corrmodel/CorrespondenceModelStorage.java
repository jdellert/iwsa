package de.jdellert.iwsa.corrmodel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
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
	
	public static CorrespondenceModel[][] loadCorrespondenceModels(ObjectInputStream in, Map<String,Integer> langToID)
			throws IOException, ClassNotFoundException {
		String langIDs[] = (String[]) in.readObject();
		int maxID = 0;
		for (int langID : langToID.values())
		{
			if (langID > maxID) maxID = langID;
		}
		PhoneticSymbolTable symbolTable = (PhoneticSymbolTable) in.readObject();
		CorrespondenceModel[][] correspondenceModels = new CorrespondenceModel[maxID + 1][maxID + 1];
		for (int fileI = 0; fileI < langIDs.length; fileI++) {
			int i = -1;
			if (langToID.get(langIDs[fileI]) != null)
			{
				i = langToID.get(langIDs[fileI]);
			}
			for (int fileJ = 0; fileJ < langIDs.length; fileJ++) {
				int j = -1;
				if (langToID.get(langIDs[fileJ]) != null)
				{
					j = langToID.get(langIDs[fileJ]);
				}
				CorrespondenceModel correspondenceModel = new CorrespondenceModel(symbolTable);
				correspondenceModel.scores = (Map<Integer, Double>) in.readObject();
				if (i >= 0 && j >=0)
				{
					correspondenceModels[i][j] = correspondenceModel;
				}
			}
		}
		return correspondenceModels;
	}
}
