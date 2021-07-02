package de.jdellert.iwsa.corrmodel;

import de.jdellert.iwsa.align.InformationWeightedSequenceAlignment;
import de.jdellert.iwsa.align.LevenshteinAlignmentAlgorithm;
import de.jdellert.iwsa.align.NeedlemanWunschAlgorithm;
import de.jdellert.iwsa.align.PhoneticStringAlignment;
import de.jdellert.iwsa.infomodel.InformationModel;
import de.jdellert.iwsa.sequence.PhoneticString;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;
import de.jdellert.iwsa.stat.CategoricalDistribution;
import de.tuebingen.sfs.cldfjava.data.CLDFForm;
import de.tuebingen.sfs.cldfjava.data.CLDFWordlistDatabase;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CognateAlignmentsWorker implements Runnable {
    CLDFWordlistDatabase database;
    PhoneticSymbolTable symbolTable;
    InformationModel[] infoModels;
    List<String> relevantParams;
    CategoricalDistribution cognatePairCorrespondenceDist;
    CorrespondenceModel globalCorr;
    List<String> langIDs;
    int numPairs;
    int numCognatePairs;

    public CognateAlignmentsWorker(CLDFWordlistDatabase database,
                                   PhoneticSymbolTable symbolTable,
                                   InformationModel[] infoModels,
                                   List<String> relevantParams) {
        this.database = database;
        this.symbolTable = symbolTable;
        this.infoModels = infoModels;
        this.relevantParams = relevantParams;
        cognatePairCorrespondenceDist = new CategoricalDistribution(symbolTable.getSize() * symbolTable.getSize());
        langIDs = database.getLangIDs();
    }

    public CognateAlignmentsWorker(CLDFWordlistDatabase database,
                                   PhoneticSymbolTable symbolTable,
                                   InformationModel[] infoModels,
                                   List<String> relevantParams,
                                   CorrespondenceModel globalCorr) {
        this.database = database;
        this.symbolTable = symbolTable;
        this.infoModels = infoModels;
        this.relevantParams = relevantParams;
        this.globalCorr = globalCorr;
        cognatePairCorrespondenceDist = new CategoricalDistribution(symbolTable.getSize() * symbolTable.getSize());
        langIDs = database.getLangIDs();
    }


    public void run() {
        for (String param : relevantParams) {
            Map<String, List<CLDFForm>> formsPerLang = database.getFormsByLanguageByParamID(param);
            if (formsPerLang == null) {
                continue;
            }
            for (String lang1 : langIDs) {
                List<CLDFForm> lang1Forms = formsPerLang.get(lang1);
                if (lang1Forms == null) {
                    continue;
                }
                for (String lang2 : langIDs) {
                    List<CLDFForm> lang2Forms = formsPerLang.get(lang2);
                    if (lang2Forms == null) {
                        continue;
                    }
                    for (CLDFForm form1inCLDF : lang1Forms) {
                        PhoneticString form1 = new PhoneticString(symbolTable.encode(form1inCLDF.getSegments()));
                        for (CLDFForm form2inCLDF : lang2Forms) {
                            PhoneticString form2 = new PhoneticString(symbolTable.encode(form2inCLDF.getSegments()));
                            PhoneticStringAlignment alignment;

                            // if a global correspondence model is given, cognate judgement is WED-based, else it is ED-based
                            if (globalCorr == null) {
                                alignment = LevenshteinAlignmentAlgorithm.constructAlignment(form1, form2);
                            } else {
                                alignment = NeedlemanWunschAlgorithm.constructAlignment(
                                        form1, form2, globalCorr, globalCorr, globalCorr, globalCorr);
                            }
                            numPairs++;
                            double[] infoScores = new double[alignment.getLength()];
                            if (infoModels == null) {
                                Arrays.fill(infoScores, 1.0);
                            }
                            else {
                                int lang1ID = langIDs.indexOf(lang1);
                                int lang2ID = langIDs.indexOf(lang2);
                                infoScores = InformationWeightedSequenceAlignment.combinedInfoScoresForAlignment(alignment, infoModels[lang1ID], infoModels[lang2ID]);
                            }
                            if ((globalCorr == null && alignment.normalizedDistanceScore <= 0.35) ||
                                    (globalCorr!= null && alignment.normalizedDistanceScore <= 0.6)) {
                                for (int pos = 0; pos < alignment.getLength(); pos++) {
                                    cognatePairCorrespondenceDist
                                            .addObservation(alignment.getSymbolPairIDAtPos(pos, symbolTable), infoScores[pos]);
                                }
                                numCognatePairs++;
                            }
                        }
                    }
                }
            }
        }
    }

    public CategoricalDistribution getCognatePairCorrespondenceDist() {
        return cognatePairCorrespondenceDist;
    }

    public int getNumCognatePairs() {
        return numCognatePairs;
    }

    public int getNumPairs() {
        return numPairs;
    }
}
