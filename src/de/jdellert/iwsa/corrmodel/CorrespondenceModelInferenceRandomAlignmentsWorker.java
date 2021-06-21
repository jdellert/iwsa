package de.jdellert.iwsa.corrmodel;

import de.jdellert.iwsa.align.InformationWeightedSequenceAlignment;
import de.jdellert.iwsa.align.LevenshteinAlignmentAlgorithm;
import de.jdellert.iwsa.align.PhoneticStringAlignment;
import de.jdellert.iwsa.infomodel.InformationModel;
import de.jdellert.iwsa.sequence.PhoneticString;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;
import de.jdellert.iwsa.stat.CategoricalDistribution;
import de.tuebingen.sfs.cldfjava.data.CLDFForm;
import de.tuebingen.sfs.cldfjava.data.CLDFWordlistDatabase;

import java.util.Arrays;
import java.util.List;

public class CorrespondenceModelInferenceRandomAlignmentsWorker implements Runnable{
    CLDFWordlistDatabase database;
    PhoneticSymbolTable symbolTable;
    InformationModel[] infoModels;
    CategoricalDistribution randomPairCorrespondenceDist;
    boolean verbose;
    int numRandomPairs;
    List<String> langIDs;
    int onePercentOfRandomPairs;

    public CorrespondenceModelInferenceRandomAlignmentsWorker(CLDFWordlistDatabase database,
                                                              PhoneticSymbolTable symbolTable,
                                                              InformationModel[] infoModels,
                                                              int numRandomPairs,
                                                              boolean verbose) {
        this.database = database;
        this.symbolTable = symbolTable;
        this.infoModels = infoModels;
        this.numRandomPairs = numRandomPairs;
        this.verbose = verbose;
        randomPairCorrespondenceDist = new CategoricalDistribution(symbolTable.getSize() * symbolTable.getSize());
        onePercentOfRandomPairs = numRandomPairs / 100;
        langIDs = database.getLangIDs();
    }

    public void run() {
        for (int i = 0; i < numRandomPairs; i++) {
            if (verbose && i % onePercentOfRandomPairs == 0) {
                System.err.println("Processed " + i + " of " + numRandomPairs + " random pairs (" + (i * 100) / numRandomPairs + "%)");
            }
            int randomLangIdx1 = (int) (Math.random() * database.getLanguageMap().size());
            int randomLangIdx2 = (int) (Math.random() * database.getLanguageMap().size());
            String randomLangId1 = langIDs.get(randomLangIdx1);
            String randomLangId2 = langIDs.get(randomLangIdx2);

            CLDFForm form1inCLDF = database.getRandomFormForLanguage(randomLangId1);
            CLDFForm form2inCLDF = database.getRandomFormForLanguage(randomLangId2);

            if (form1inCLDF == null || form2inCLDF == null) {
                i--;
                continue;
            }

            // get the segments from the CLDFForm, encode them as ints and construct PhoneticString objects
            PhoneticString form1 = new PhoneticString(symbolTable.encode(form1inCLDF.getSegments()));
            PhoneticString form2 = new PhoneticString(symbolTable.encode(form2inCLDF.getSegments()));
            PhoneticStringAlignment alignment = LevenshteinAlignmentAlgorithm.constructAlignment(form1, form2);
            double[] infoScores = new double[alignment.getLength()];
            if (infoModels == null) {
                Arrays.fill(infoScores, 1.0);
            } else {
                infoScores = InformationWeightedSequenceAlignment.combinedInfoScoresForAlignment(alignment, infoModels[randomLangIdx1], infoModels[randomLangIdx2]);
            }
            for (int pos = 0; pos < alignment.getLength(); pos++) {
                randomPairCorrespondenceDist.addObservation(alignment.getSymbolPairIDAtPos(pos, symbolTable), infoScores[pos]);
            }
        }
    }

    public CategoricalDistribution getRandomPairCorrespondenceDist() {
        return randomPairCorrespondenceDist;
    }
}
