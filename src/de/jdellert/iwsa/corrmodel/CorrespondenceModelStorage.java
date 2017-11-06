package de.jdellert.iwsa.corrmodel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

import de.jdellert.iwsa.sequence.PhoneticSymbolTable;

public class CorrespondenceModelStorage {
	public static void writeModelToObjectStream(CorrespondenceModel model, ObjectOutputStream stream)
			throws IOException {
		stream.writeObject(model.symbolTable);
		stream.writeObject(model.scores);
	}
	public static void writeGlobalModelToFile(CorrespondenceModel globalCorrModel, String fileName)
			throws FileNotFoundException, IOException {
		System.err.print("Writing correspondence model to " + fileName + " ...");
		ObjectOutputStream outputStream = new ObjectOutputStream(
				new FileOutputStream(new File(fileName)));
		writeModelToObjectStream(globalCorrModel, outputStream);
		outputStream.close();
		System.err.println("done.");
	}
	

	public static void writeLocalModelsToFile(CorrespondenceModel[][] localCorrModels, String[] langIDs, String fileName)
			throws FileNotFoundException, IOException {
		System.err.print("Writing correspondence models to " + fileName);
		ObjectOutputStream outputStream = new ObjectOutputStream(
				new FileOutputStream(new File(fileName)));
		outputStream.writeObject(langIDs);
		for (int i = 0; i < langIDs.length; i++) {
			for (int j = 0; j < langIDs.length; j++) {
				writeModelToObjectStream(localCorrModels[i][j], outputStream);
			}
		}
		outputStream.close();
		System.err.println("done.");
	}
	
	public static CorrespondenceModel loadCorrespondenceModel(ObjectInputStream in)
			throws IOException, ClassNotFoundException {
		PhoneticSymbolTable symbolTable = (PhoneticSymbolTable) in.readObject();
		Map<Integer,Double> scores = (Map<Integer,Double>) in.readObject();	
		CorrespondenceModel correspondenceModel = new CorrespondenceModel(symbolTable);
		correspondenceModel.scores = scores;
		return correspondenceModel;
	}

	public static CorrespondenceModel[][] loadCorrespondenceModels(ObjectInputStream in)
			throws IOException, ClassNotFoundException {
		String langIDs[] = (String[]) in.readObject();
		//TODO: the same symbol table gets copied during import, store it only once in the global file!
		CorrespondenceModel[][] correspondenceModels = new CorrespondenceModel[langIDs.length][langIDs.length];
		for (int i = 0; i < langIDs.length; i++) {
			for (int j = 0; j < langIDs.length; j++) {
				CorrespondenceModel correspondenceModel = (CorrespondenceModel) in.readObject();
				correspondenceModels[i][j] = correspondenceModel;
			}
		}
		return correspondenceModels;
	}
}
