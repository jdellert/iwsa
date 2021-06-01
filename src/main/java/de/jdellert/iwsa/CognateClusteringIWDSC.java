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
import de.jdellert.iwsa.data.CLDFImport;
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
            String resultFileName = args[1];

            LexicalDatabase database = CLDFImport.loadDatabase(args[0], true);
            PhoneticSymbolTable symbolTable = database.getSymbolTable();

            InformationModel[] infoModels = InformationModelInference.inferInformationModels(database, symbolTable);

            // default: assume all languages are relevant, and part of the inference
            String[] relevantLangCodes = database.getLanguageCodes();
            int[] relevantLangIDs = new int[database.getNumLanguages()];
            for (int i = 0; i < relevantLangIDs.length; i++) {
                relevantLangIDs[i] = i;
            }

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
                CorrespondenceModelStorage.serializeLocalModelsToFile(localCorrModels, database.getLanguageCodes(),
                        symbolTable, args[0] + "-local-iw.corr");
            }

            for (int conceptID = 0; conceptID < database.getNumConcepts(); conceptID++) {
                System.err.println("Clustering words for concept #" + conceptID + " (" + database.getConceptName(conceptID) + ")");

                // build distance matrix
                List<Integer> formIDs = database.getFormIDsForConcept(conceptID);
                Map<Integer, Integer> formIDToIndex = new TreeMap<Integer, Integer>();
                for (int index = 0; index < formIDs.size(); index++) {
                    formIDToIndex.put(formIDs.get(index), index);
                }

                double[][] distanceMatrix = new double[formIDs.size()][formIDs.size()];

                List<List<Integer>> formsPerLang = database.getFormIDsForConceptPerLanguage(conceptID);
                for (int i = 0; i < relevantLangIDs.length; i++)
                {
                    int lang1ID = relevantLangIDs[i];
                    for (int j = i; j < relevantLangIDs.length; j++) {
                        int lang2ID = relevantLangIDs[j];
                        for (int lang1FormID : formsPerLang.get(lang1ID)) {
                            int index1 = formIDToIndex.get(lang1FormID);
                            PhoneticString lang1Form = database.getForm(lang1FormID);
                            for (int lang2FormID : formsPerLang.get(lang2ID)) {
                                int index2 = formIDToIndex.get(lang2FormID);
                                PhoneticString lang2Form = database.getForm(lang2FormID);
                                PhoneticStringAlignment localWeightsAlignment = InformationWeightedSequenceAlignment
                                        .constructAlignment(lang1Form, lang2Form, globalCorrModel,
                                                localCorrModels[lang1ID][lang2ID], localCorrModels[lang1ID][lang1ID],
                                                localCorrModels[lang2ID][lang2ID], infoModels[lang1ID],
                                                infoModels[lang2ID]);
                                double localWeightDistance = localWeightsAlignment.normalizedDistanceScore;
                                if (localWeightDistance < 0.0) localWeightDistance = 0.0;
                                localWeightDistance *= localWeightDistance;
                                if (localWeightDistance > MAX_DIST_VAL) localWeightDistance = MAX_DIST_VAL;
                                localWeightDistance /= MAX_DIST_VAL;
                                if (index1 == index2) localWeightDistance = 0.0;
                                distanceMatrix[index1][index2] = localWeightDistance;
                                distanceMatrix[index2][index1] = localWeightDistance;
                            }
                        }
                    }
                }

                // UPGMA clustering
                Set<Set<Integer>> cognateSets = FlatClustering.upgma(distanceMatrix, THRESHOLD);

                // store cluster IDs in database
                for (Set<Integer> cognateSet : cognateSets) {
                    List<Integer> cognateSetFormIDs = new ArrayList<Integer>(cognateSet.size());
                    for (Integer index : cognateSet) {
                        int formID = formIDs.get(index);
                        cognateSetFormIDs.add(formID);
                    }
                    database.addCognateSet(cognateSetFormIDs);
                }
            }

            // CLDF output (without header by default)
            CLDFExport.exportToFile(database, resultFileName, false);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
