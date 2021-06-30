package de.jdellert.iwsa.corrmodel;

import de.jdellert.iwsa.infomodel.InformationModel;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;
import de.tuebingen.sfs.cldfjava.data.CLDFWordlistDatabase;

import java.util.List;

public class SelfCorrespondenceWorker implements Runnable {
    CLDFWordlistDatabase database;
    PhoneticSymbolTable symbolTable;
    List<String> relevantLangIDs;
    CorrespondenceModel globalCorr;
    InformationModel[] infoModels;
    CorrespondenceModel[] selfCorrespondences;

    public SelfCorrespondenceWorker(CLDFWordlistDatabase database, PhoneticSymbolTable symbolTable,
                                     List<String> relevantLangIDs, CorrespondenceModel globalCorr, InformationModel[] infoModels) {
        this.database = database;
        this.symbolTable = symbolTable;
        this.relevantLangIDs = relevantLangIDs;
        this.globalCorr = globalCorr;
        this.infoModels = infoModels;
        this.selfCorrespondences = new CorrespondenceModel[relevantLangIDs.size()];
    }

    public void run() {
        for (String langID : relevantLangIDs) {
            int langIdx = relevantLangIDs.indexOf(langID);
            System.err.println("Storing local correspondence model for " + langID + " and " + langID);
            selfCorrespondences[langIdx] = CorrespondenceModelInference.inferCorrModelForPair(database, symbolTable, langID, langID,
                    globalCorr, globalCorr, globalCorr, infoModels[langIdx], infoModels[langIdx]);
        }
    }

    public CorrespondenceModel[] getSelfCorrespondences() {
        return selfCorrespondences;
    }
}
