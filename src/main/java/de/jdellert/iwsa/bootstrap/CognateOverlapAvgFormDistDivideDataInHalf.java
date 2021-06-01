package de.jdellert.iwsa.bootstrap;

import de.jdellert.iwsa.CognateClusteringIWDSC;
import de.jdellert.iwsa.align.InformationWeightedSequenceAlignment;
import de.jdellert.iwsa.align.PhoneticStringAlignment;
import de.jdellert.iwsa.cluster.FlatClustering;
import de.jdellert.iwsa.corrmodel.CorrespondenceModel;
import de.jdellert.iwsa.corrmodel.CorrespondenceModelInference;
import de.jdellert.iwsa.corrmodel.CorrespondenceModelStorage;
import de.jdellert.iwsa.data.CLDFImport;
import de.jdellert.iwsa.data.LexicalDatabase;
import de.jdellert.iwsa.infomodel.InformationModel;
import de.jdellert.iwsa.infomodel.InformationModelInference;
import de.jdellert.iwsa.sequence.PhoneticString;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;
import de.jdellert.iwsa.util.io.SimpleFormatReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * This script prepares distance matrices based on IWSA for doing 2-fold cross-validation and related procedures such as
 * the CLARITY resampling significance testing. For both phonetic and cognate-overlap data, we create n paired samples by
 * dividing the original data into two LexicalDatabase-s, and then inferring local models on them independently with the
 * help of the same global correspondence model. The idea is that each half of the data should carry approximately the same
 * historical-linguistic signal, and comparing how similar the pairs of matrices resulting from two different data halves are,
 * helps us to estimate the natural variance of our inference.
 * <p>
 * Usage: java -cp . de.jdellert.iwsa.bootstrap.CognateOverlapAvgFormDistDivideDataInHalf YOURPATH/northeuralex-0.9-cldf.tsv YOURPATH/nelex-0.9-langs-ie.txt 21 30
 *
 * @author igoryanovich
 */

public class CognateOverlapAvgFormDistDivideDataInHalf {
    public static final NumberFormat formatter = new DecimalFormat("0.0000", new DecimalFormatSymbols(Locale.ENGLISH));
    public static final NumberFormat paddingFormatter = new DecimalFormat("0000", new DecimalFormatSymbols(Locale.ENGLISH));
    public static int START_SAMPLE_ID = 0;
    public static int LAST_SAMPLE_ID = 100;

    public static void main(String[] args) {
        try {
            LexicalDatabase database = CLDFImport.loadDatabase(args[0], true);
            PhoneticSymbolTable symbolTable = database.getSymbolTable();

            InformationModel[] infoModels = InformationModelInference.inferInformationModels(database, symbolTable);

            // default: assume all languages are relevant, and part of the inference
            String[] relevantLangCodes = database.getLanguageCodes();
            int[] relevantLangIDs = new int[database.getNumLanguages()];
            for (int i = 0; i < relevantLangIDs.length; i++) {
                relevantLangIDs[i] = i;
            }

            // interpret additional argument as a list of language IDs
            if (args.length > 1) {
                relevantLangCodes = SimpleFormatReader.listFromFile(args[1]).stream().toArray(String[]::new);
                relevantLangIDs = new int[relevantLangCodes.length];

                for (int i = 0; i < relevantLangCodes.length; i++) {
                    int langID = database.getIDForLanguageCode(relevantLangCodes[i]);
                    if (langID == -1) {
                        System.err.println("ERROR: language code " + relevantLangCodes[i] + " does not occur in database!");
                        System.exit(1);
                    }
                    relevantLangIDs[i] = langID;
                }
            }

            if (args.length > 2) {
                START_SAMPLE_ID = Integer.parseInt(args[2]);
            }

            if (args.length > 3) {
                LAST_SAMPLE_ID = Integer.parseInt(args[3]);
            }

            Map<String, Integer> relevantLangToID = new TreeMap<String, Integer>();
            for (int langID = 0; langID < relevantLangIDs.length; langID++) {
                relevantLangToID.put(relevantLangCodes[langID], relevantLangIDs[langID]);
            }

            CorrespondenceModel globalCorrModel = null;
            try {
                System.err.print("Attempting to load existing global correspondence model from " + args[0]
                        + "-global-iw.corr ... ");
                globalCorrModel = CorrespondenceModelStorage
                        .deserializeCorrespondenceModel(new ObjectInputStream(new FileInputStream(args[0] + "-global-iw.corr")));
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
                globalCorrModel = CorrespondenceModelInference.inferGlobalCorrespondenceModel(database, symbolTable, infoModels);
                CorrespondenceModelStorage.writeGlobalModelToFile(globalCorrModel, args[0] + "-global-iw.corr");
            }

            if (START_SAMPLE_ID == 0) {
                produceMatricesForSample(args[0] + "-mtx-originaldata.tsv", database, relevantLangIDs, globalCorrModel, infoModels);
                START_SAMPLE_ID = 1;
            }

            for (int k = START_SAMPLE_ID; k <= LAST_SAMPLE_ID; k++) {
                //LexicalDatabase sample = new LexicalDatabaseConceptBootstrapSample(database);
                produceMatricesFromHalvedData(args[0] + "-mtx-sample" + paddingFormatter.format(k) + paddingFormatter.format(1) + ".tsv",
                        args[0] + "-mtx-sample" + paddingFormatter.format(k) + paddingFormatter.format(2) + ".tsv",
                        database, relevantLangIDs, globalCorrModel, infoModels);
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void produceMatricesFromHalvedData(String fileName1, String fileName2, LexicalDatabase database, int[] relevantLangIDs, CorrespondenceModel globalCorrModel, InformationModel[] infoModels) throws IOException {
        //this function divides the data in two halves by concept, and infers local correspondences independently for each half

        //get the original concept list, shuffle it
        int numConcepts = database.getNumConcepts();
        List<Integer> conceptIDs = new ArrayList<Integer>();
        String[] conceptNames = new String[numConcepts];
        for (int i = 0; i < numConcepts; i++) {
            conceptIDs.add(i);
            conceptNames[i] = database.getConceptName(i);
        }
        Collections.shuffle(conceptIDs);

        String[] firstHalfOfConceptNames = new String[numConcepts / 2];
        String[] secondHalfOfConceptNames = new String[numConcepts - numConcepts / 2];
        for (int i = 0; i < numConcepts / 2; i++) {
            firstHalfOfConceptNames[i] = conceptNames[conceptIDs.get(i)];
        }
        for (int i = numConcepts / 2; i < numConcepts; i++) {
            secondHalfOfConceptNames[i - numConcepts / 2] = conceptNames[conceptIDs.get(i)];
        }

        System.err.print(firstHalfOfConceptNames.toString());

        int[] firstHalfOfConceptIDs = new int[numConcepts / 2];
        int[] secondHalfOfConceptIDs = new int[numConcepts - numConcepts / 2];
        for (int i = 0; i < numConcepts / 2; i++) {
            firstHalfOfConceptIDs[i] = conceptIDs.get(i);
        }
        for (int i = numConcepts / 2; i < numConcepts; i++) {
            secondHalfOfConceptIDs[i - numConcepts / 2] = conceptIDs.get(i);
        }

        System.err.print(firstHalfOfConceptIDs.toString());


        //build a lexical database from the first half, compute and save the distance matrices
        LexicalDatabase firstHalfDatabase = new LexicalDatabase(database.getSymbolTable(), database.langCodes, firstHalfOfConceptNames);
        copyConceptsFromDatabaseToDatabase(database, firstHalfDatabase, firstHalfOfConceptIDs);
        produceMatricesForSample(fileName1, firstHalfDatabase, relevantLangIDs, globalCorrModel, infoModels);

        //similarly for the second half
        LexicalDatabase secondHalfDatabase = new LexicalDatabase(database.getSymbolTable(), database.langCodes, secondHalfOfConceptNames);
        copyConceptsFromDatabaseToDatabase(database, secondHalfDatabase, secondHalfOfConceptIDs);
        produceMatricesForSample(fileName2, secondHalfDatabase, relevantLangIDs, globalCorrModel, infoModels);
    }

    public static void copyConceptsFromDatabaseToDatabase(LexicalDatabase source, LexicalDatabase target, int[] conceptsToCopy) {
        int numLanguages = source.getNumLanguages();
        for (int conceptID : conceptsToCopy) {
            for (int langID = 0; langID < numLanguages; langID++) {
                List<Integer> formIDsToCopy = source.getFormIDsForLanguageAndConcept(langID, conceptID);
                for (Integer formID : formIDsToCopy) {
                    target.addForm(source.getLanguageCode(langID), source.getConceptName(conceptID), source.getForm(formID));
                }
            }
        }
    }

    public static void produceMatricesForSample(String fileName, LexicalDatabase database, int[] relevantLangIDs, CorrespondenceModel globalCorrModel, InformationModel[] infoModels) throws IOException {
        //infer local correspondences for all necessary language pairs
        System.err.print(fileName + ", stage 1: inference of sound correspondence matrices for each language pair\n");
        CorrespondenceModel localCorrModels[][] = CorrespondenceModelInference.inferLocalCorrespondenceModels(database, database.getSymbolTable(), relevantLangIDs, globalCorrModel, infoModels);

        List<Map<Integer, Integer>> formsToCognateSetPerConcept = new ArrayList<Map<Integer, Integer>>(database.getNumConcepts());
        List<Double[][]> formDistances = new ArrayList<Double[][]>(database.getNumConcepts());
        System.err.print(fileName + ", stage 2: computing form distance matrix and cognate clustering\n");
        aggregateAnalysis(formsToCognateSetPerConcept, formDistances, database, relevantLangIDs, globalCorrModel, localCorrModels, infoModels);

        FileWriter out = new FileWriter(new File(fileName));
        for (int i = 0; i < relevantLangIDs.length; i++) {
            int lang1ID = relevantLangIDs[i];
            for (int j = 0; j < relevantLangIDs.length; j++) {
                int lang2ID = relevantLangIDs[j];

                double cogOverlap = computeCognateOverlap(formsToCognateSetPerConcept, lang1ID, lang2ID, database);
                double avgFrmDist = computeAverageFormDistance(formDistances, i, j);
                out.write(database.getLanguageCode(lang1ID) + "\t" + database.getLanguageCode(lang2ID) + "\t" + formatter.format(cogOverlap) + "\t" + formatter.format(avgFrmDist) + "\n");
            }
        }
        out.close();
    }

    public static void aggregateAnalysis(List<Map<Integer, Integer>> formsToCognateSetPerConcept, List<Double[][]> formDistances, LexicalDatabase database, int[] relevantLangIDs, CorrespondenceModel globalCorrModel, CorrespondenceModel localCorrModels[][], InformationModel[] infoModels) {
        for (int conceptID = 0; conceptID < database.getNumConcepts(); conceptID++) {
            System.err.println("Clustering words for concept #" + conceptID + " (" + database.getConceptName(conceptID) + ")");

            List<List<Integer>> formsPerLang = database.getFormIDsForConceptPerLanguage(conceptID);

            // build distance matrix
            List<Integer> formIDs = new LinkedList<Integer>();
            for (int langID : relevantLangIDs) {
                formIDs.addAll(formsPerLang.get(langID));
            }
            Map<Integer, Integer> formIDToIndex = new TreeMap<Integer, Integer>();
            for (int index = 0; index < formIDs.size(); index++) {
                formIDToIndex.put(formIDs.get(index), index);
            }

            double[][] distanceMatrix = new double[formIDs.size()][formIDs.size()];
            Double[][] minDistanceMatrix = new Double[relevantLangIDs.length][relevantLangIDs.length];
            for (Double[] row : minDistanceMatrix) {
                Arrays.fill(row, Double.POSITIVE_INFINITY);
            }

            for (int i = 0; i < relevantLangIDs.length; i++) {
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
                            if (localWeightDistance > CognateClusteringIWDSC.MAX_DIST_VAL)
                                localWeightDistance = CognateClusteringIWDSC.MAX_DIST_VAL;
                            localWeightDistance /= CognateClusteringIWDSC.MAX_DIST_VAL;
                            if (index1 == index2) localWeightDistance = 0.0;
                            distanceMatrix[index1][index2] = localWeightDistance;
                            distanceMatrix[index2][index1] = localWeightDistance;
                            if (localWeightDistance < minDistanceMatrix[i][j]) {
                                minDistanceMatrix[i][j] = localWeightDistance;
                                minDistanceMatrix[j][i] = localWeightDistance;
                            }
                        }
                    }
                }
            }

            formDistances.add(minDistanceMatrix);

            // UPGMA clustering
            Set<Set<Integer>> cognateSets = FlatClustering.upgma(distanceMatrix, CognateClusteringIWDSC.THRESHOLD);

            Map<Integer, Integer> formsToCognateSet = new TreeMap<Integer, Integer>();
            int cogSetID = 0;
            for (Set<Integer> cognateSet : cognateSets) {
                for (Integer id : cognateSet) {
                    formsToCognateSet.put(formIDs.get(id), cogSetID);
                    //System.err.println(database.getForm(formIDs.get(id)).toUntokenizedString(database.getSymbolTable()));
                }
                cogSetID++;
                //System.err.println();
            }

            formsToCognateSetPerConcept.add(formsToCognateSet);

        }
    }

    public static double computeCognateOverlap(List<Map<Integer, Integer>> formsToCognateSetPerConcept, int lang1ID, int lang2ID, LexicalDatabase database) {
        double lang1FormNum = 0.0;
        double lang2FormNum = 0.0;
        double lang1lang2SharedSets = 0.0;
        for (int conceptID = 0; conceptID < database.getNumConcepts(); conceptID++) {
            Map<Integer, Integer> formToCognateSetID = formsToCognateSetPerConcept.get(conceptID);
            List<Integer> lang1FormIDs = database.getFormIDsForLanguageAndConcept(lang1ID, conceptID);
            List<Integer> lang2FormIDs = database.getFormIDsForLanguageAndConcept(lang2ID, conceptID);
            lang1FormNum += lang1FormIDs.size();
            lang2FormNum += lang2FormIDs.size();
            for (int lang1FormID : lang1FormIDs) {
                int cogSetID = formToCognateSetID.get(lang1FormID);
                boolean foundCognate = false;
                for (int lang2FormID : lang2FormIDs) {
                    if (formToCognateSetID.get(lang2FormID) == cogSetID) {
                        foundCognate = true;
                        break;
                    }
                }
                if (foundCognate) {
                    lang1lang2SharedSets += 1.0;
                }
            }
        }
        return lang1lang2SharedSets / Math.max(lang1FormNum, lang2FormNum);
    }

    public static double computeAverageFormDistance(List<Double[][]> formDistances, int i, int j) {
        double sum = 0;
        int numEntries = 0;
        for (int k = 0; k < formDistances.size(); k++) {
            double val = formDistances.get(k)[i][j];
            if (val < Double.POSITIVE_INFINITY) {
                sum += val;
                numEntries += 1;
            }
        }
        return sum / numEntries;
    }
}
