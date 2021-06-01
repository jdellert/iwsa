package de.jdellert.iwsa.corrmodel;

import java.io.*;
import java.util.*;

import de.jdellert.iwsa.sequence.PhoneticSymbolTable;
import de.tuebingen.sfs.eie.shared.util.bin.BufferedByteReader;
import de.tuebingen.sfs.eie.shared.util.bin.IOUtils;

public class CorrespondenceModelStorage {

    public static void writeGlobalModelToFile(CorrespondenceModel globalCorrModel, String fileName) {
        System.err.print("Writing global correspondence model to " + fileName + " ...");
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(fileName)))) {
            Iterator<String> symbolIter = globalCorrModel.symbolTable.iterator();
            while (symbolIter.hasNext())
                IOUtils.writeAsBytes(symbolIter.next(), out);
            IOUtils.writeNewline(out);

            for (Map.Entry<Integer, Double> next : globalCorrModel.scores.entrySet()) {
                IOUtils.writeInt(next.getKey(), out);
                IOUtils.writeDouble(next.getValue(), out);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.err.println("done.");
    }

    public static CorrespondenceModel readGlobalModelFromFile(String fileName) {
        List<String> symbols = new ArrayList<>();
        Map<Integer, Double> scores = new HashMap<>();

        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(new File(fileName)))) {
            BufferedByteReader reader = new BufferedByteReader(in);

            while (!reader.startsWithNewline())
                symbols.add(reader.nextString());
            reader.skip(2);

            while (reader.hasNext()) {
                int sym = reader.popToInt();
                double score = reader.popToDouble();
                scores.put(sym, score);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        CorrespondenceModel corrModel = new CorrespondenceModel(new PhoneticSymbolTable(symbols));
        corrModel.scores = scores;
        return corrModel;
    }

    public static void serializeGlobalModelToFile(CorrespondenceModel globalCorrModel, String fileName)
            throws FileNotFoundException, IOException {
        System.err.print("Writing global correspondence model to " + fileName + " ...");
        ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(new File(fileName)));
        outputStream.writeObject(globalCorrModel.symbolTable);
        outputStream.writeObject(globalCorrModel.scores);
        outputStream.close();
        System.err.println("done.");
    }

    public static void serializeLocalModelsToFile(CorrespondenceModel[][] localCorrModels, String[] langIDs,
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

    public static CorrespondenceModel deserializeCorrespondenceModel(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        PhoneticSymbolTable symbolTable = (PhoneticSymbolTable) in.readObject();
        Map<Integer, Double> scores = (Map<Integer, Double>) in.readObject();
        CorrespondenceModel correspondenceModel = new CorrespondenceModel(symbolTable);
        correspondenceModel.scores = scores;
        return correspondenceModel;
    }

    public static CorrespondenceModel[][] deserializeCorrespondenceModels(ObjectInputStream in)
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

    public static CorrespondenceModel[][] deserializeCorrespondenceModels(ObjectInputStream in, Map<String,Integer> langToID)
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