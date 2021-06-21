package de.jdellert.iwsa;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import de.jdellert.iwsa.align.InformationWeightedSequenceAlignment;
import de.jdellert.iwsa.align.PhoneticStringAlignment;
import de.jdellert.iwsa.align.PhoneticStringAlignmentOutput;
import de.jdellert.iwsa.cluster.FlatClustering;
import de.jdellert.iwsa.corrmodel.CorrespondenceModel;
import de.jdellert.iwsa.corrmodel.CorrespondenceModelInference;
import de.jdellert.iwsa.corrmodel.CorrespondenceModelStorage;
import de.jdellert.iwsa.data.CLDFExport;
//import de.jdellert.iwsa.data.CLDFImport;
import de.tuebingen.sfs.cldfjava.data.CLDFForm;
import de.tuebingen.sfs.cldfjava.data.CLDFLanguage;
import de.tuebingen.sfs.cldfjava.data.CLDFWordlistDatabase;
import de.tuebingen.sfs.cldfjava.io.CLDFImport;
import de.jdellert.iwsa.data.LexicalDatabase;
import de.jdellert.iwsa.infomodel.InformationModel;
import de.jdellert.iwsa.infomodel.InformationModelInference;
import de.jdellert.iwsa.sequence.PhoneticString;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;

public class CognateClusteringIWDSC {
    public static final double THRESHOLD = 0.75;
    public static final double MAX_DIST_VAL = 1.5;

    public static void main(String[] args) {
        try {
            //String resultFileName = args[1];

            CLDFWordlistDatabase database = CLDFImport.loadDatabase(args[0]);
            PhoneticSymbolTable symbolTable = PhoneticSymbolTable.symbolTableFromDatabase(database);

            InformationModel[] infoModels = InformationModelInference.inferInformationModels(database, symbolTable);

            // default: assume all languages are relevant, and part of the inference
            Map<String, CLDFLanguage> languageMap = database.getLanguageMap();
            List<String> relevantLangIDs = database.getLangIDs();
            Map<String, Integer> relevantLangToID = new TreeMap<String, Integer>();
            for (int i = 0; i < relevantLangIDs.size(); i++) {
                relevantLangToID.put(relevantLangIDs.get(i), i);
            }

            /*String[] langIDs = new String[languageMap.size()];
            CLDFLanguage[] languages = new CLDFLanguage[languageMap.size()];
            int i = 0;
            for (Map.Entry<String, CLDFLanguage> languageEntry : languageMap.entrySet()) {
                langIDs[i] = languageEntry.getKey();
                languages[i] = languageEntry.getValue();
                i++;
            }*/



            /*

            // interpret additional arguments as language IDs
            if (args.length > 2) {
                relevantLangCodes = new String[args.length - 2];
                relevantLangIDs = new int[args.length - 2];
                for (int i = 2; i < args.length; i++) {
                    int langID = database.getIDForLanguageCode(args[i]);
                    if (langID == -1) {
                        System.err.println("ERROR: language code " + args[i] + " does not occur in database!");
                        System.exit(1);
                    }
                    relevantLangCodes[i - 2] = args[i];
                    relevantLangIDs[i - 2] = langID;
                }
            }



            Map<String, Integer> relevantLangToID = new TreeMap<String, Integer>();
            for (int langID = 0; langID < relevantLangIDs.length; langID++) {
                relevantLangToID.put(relevantLangCodes[langID], relevantLangIDs[langID]);
            }

             */

            CorrespondenceModel globalCorrModel = null;
            try {
                System.err.print("Attempting to load existing global correspondence model from " + args[0]
                        + "-global-iw.corr ... ");
                globalCorrModel = CorrespondenceModelStorage.deserializeCorrespondenceModel(
                        new ObjectInputStream(new FileInputStream(args[0] + "-global-iw.corr")));
                System.err.print(
                        "done.\nStage 1: Global sound correspondences - skipped because previously inferred model was found. Delete model file and rerun to cause re-inference.\n");
            } catch (FileNotFoundException e) {
                System.err.print(" file not found, need to infer global model first.\n");
            } catch (IOException e) {
                System.err.print(" format error, need to reinfer global model.\n");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                System.exit(0);
            }

            if (globalCorrModel == null) {
                System.err.print("Stage 1: Inference of global PMI scores\n");
                globalCorrModel = CorrespondenceModelInference.inferGlobalCorrespondenceModel(database, symbolTable,
                        infoModels);
                CorrespondenceModelStorage.serializeGlobalModelToFile(globalCorrModel, args[0] + "-global-iw.corr");
            }

            CorrespondenceModel[][] localCorrModels = null;
            try {
                System.err.print("Attempting to load existing local correspondence models from " + args[0]
                        + "-local-iw.corr ... ");
                localCorrModels = CorrespondenceModelStorage.deserializeCorrespondenceModels(
                        new ObjectInputStream(new FileInputStream(args[0] + "-local-iw.corr")), relevantLangToID);
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
            if (localCorrModels == null) {
                System.err.print("Stage 2: Inference of sound correspondence matrices for each language pair\n");
                localCorrModels = CorrespondenceModelInference.inferLocalCorrespondenceModels(database, symbolTable,
                        relevantLangIDs, globalCorrModel, infoModels);
                String[] langIDsArray = new String[relevantLangIDs.size()];
                relevantLangIDs.toArray(langIDsArray);
                CorrespondenceModelStorage.serializeLocalModelsToFile(localCorrModels, langIDsArray,
                        symbolTable, args[0] + "-local-iw.corr");
            }

            int conceptCounter = 0;
            for (String concept : database.getConceptMap().keySet()) {
                System.err.println("Clustering words for concept #" + conceptCounter + " (" + concept + ")");

                // build distance matrix
                //List<CLDFForm> forms = database.getFormsByParamID(concept);
                Map<Integer, CLDFForm> formMap = database.getFormsMap();
                double[][] distanceMatrix = new double[formMap.size()][formMap.size()];

                Map<String, List<CLDFForm>> formsPerLang = database.getFormsByLanguageByParamID(concept);
                for (int i = 0; i < relevantLangIDs.size(); i++) {
                    String lang1 = relevantLangIDs.get(i);
                    int lang1ID = relevantLangIDs.indexOf(lang1);
                    List<CLDFForm> lang1Forms = formsPerLang.get(lang1);
                    for (int j = i; j < relevantLangIDs.size(); j++) {
                        String lang2 = relevantLangIDs.get(j);
                        int lang2ID = relevantLangIDs.indexOf(lang2);
                        List<CLDFForm> lang2Forms = formsPerLang.get(lang2);
                        for (CLDFForm form1inCLDF : lang1Forms) {
                            PhoneticString form1 = new PhoneticString(symbolTable.encode(form1inCLDF.getSegments()));
                            for (CLDFForm form2inCLDF : lang2Forms) {
                                PhoneticString form2 = new PhoneticString(symbolTable.encode(form2inCLDF.getSegments()));
                                PhoneticStringAlignment localWeightsAlignment = InformationWeightedSequenceAlignment
                                        .constructAlignment(form1, form2, globalCorrModel,
                                                localCorrModels[lang1ID][lang2ID], localCorrModels[lang1ID][lang1ID],
                                                localCorrModels[lang2ID][lang2ID], infoModels[lang1ID],
                                                infoModels[lang2ID]);
                                double localWeightDistance = localWeightsAlignment.normalizedDistanceScore;
                                if (localWeightDistance < 0.0) localWeightDistance = 0.0;
                                localWeightDistance *= localWeightDistance;
                                if (localWeightDistance > MAX_DIST_VAL) localWeightDistance = MAX_DIST_VAL;
                                localWeightDistance /= MAX_DIST_VAL;
                                if (lang1ID == lang2ID) localWeightDistance = 0.0;
                                distanceMatrix[lang1ID][lang2ID] = localWeightDistance;
                                distanceMatrix[lang2ID][lang1ID] = localWeightDistance;
                            }
                        }
                    }
                }
                // UPGMA clustering
                Set<Set<Integer>> cognateSets = FlatClustering.upgma(distanceMatrix, THRESHOLD);


                /*
                // store cluster IDs in database
                for (Set<Integer> cognateSet : cognateSets) {
                    List<Integer> cognateSetFormIDs = new ArrayList<Integer>(cognateSet.size());
                    for (Integer index : cognateSet) {
                        int formID = formIDs.get(index);
                        cognateSetFormIDs.add(formID);
                    }
                    database.addCognateSet(cognateSetFormIDs);
                }*/

                conceptCounter++;
            }



            // CLDF output (without header by default)
            // CLDFExport.exportToFile(database, resultFileName, false);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
