package de.jdellert.iwsa.corrmodel;

import de.jdellert.iwsa.infomodel.InformationModel;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;
import de.tuebingen.sfs.cldfjava.data.CLDFWordlistDatabase;

import java.util.List;

public class LocalCorrespondenceWorker implements Runnable{
    CLDFWordlistDatabase database;
    PhoneticSymbolTable symbolTable;
    List<String> allRelevantLangIDs;
    List<String> localRelevantLangIDs;
    CorrespondenceModel globalCorr;
    InformationModel[] infoModels;
    CorrespondenceModel[] selfCorrModels;
    CorrespondenceModel[][] localCorrModels;

    public LocalCorrespondenceWorker(CLDFWordlistDatabase database, PhoneticSymbolTable symbolTable, List<String> allRelevantLangIDs,
                                     List<String> localRelevantLangIDs, CorrespondenceModel globalCorr,
                                     CorrespondenceModel[] selfCorrModels, InformationModel[] infoModels) {
        this.database = database;
        this.symbolTable = symbolTable;
        this.allRelevantLangIDs = allRelevantLangIDs;
        this.localRelevantLangIDs = localRelevantLangIDs;
        this.globalCorr = globalCorr;
        this.infoModels = infoModels;
        this.selfCorrModels = selfCorrModels;
        this.localCorrModels = new CorrespondenceModel[localRelevantLangIDs.size()][allRelevantLangIDs.size()];
    }


    public void run() {
        for (String lang1ID : localRelevantLangIDs) {
            int lang1LocalIdx = localRelevantLangIDs.indexOf(lang1ID);
            int lang1GlobalIdx = allRelevantLangIDs.indexOf(lang1ID);
            for (String lang2ID : allRelevantLangIDs) {
                int lang2Idx = allRelevantLangIDs.indexOf(lang2ID);
                if (lang1GlobalIdx == lang2Idx) {
                    continue;
                }
                System.err.println("Storing local correspondence model for " + lang1ID + " and " + lang2ID);
                localCorrModels[lang1LocalIdx][lang2Idx] = CorrespondenceModelInference.inferCorrModelForPair(database, symbolTable, lang1ID, lang2ID,
                        globalCorr, selfCorrModels[lang1GlobalIdx], selfCorrModels[lang2Idx], infoModels[lang1GlobalIdx], infoModels[lang2Idx]);
            }
        }
    }

    public CorrespondenceModel[][] getLocalCorrModels() {
        return localCorrModels;
    }
}
