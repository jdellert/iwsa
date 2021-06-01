package de.jdellert.iwsa;

import de.jdellert.iwsa.align.InformationWeightedSequenceAlignment;
import de.jdellert.iwsa.data.CLDFImport;
import de.jdellert.iwsa.data.LexicalDatabase;
import de.jdellert.iwsa.infomodel.InformationModel;
import de.jdellert.iwsa.infomodel.InformationModelInference;
import de.jdellert.iwsa.sequence.PhoneticString;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class InformationLengthPerWordOutput {
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

            // interpret additional arguments as language IDs
            if (args.length > 1) {
                relevantLangCodes = new String[args.length - 1];
                relevantLangIDs = new int[args.length - 1];
                for (int i = 1; i < args.length; i++) {
                    int langID = database.getIDForLanguageCode(args[i]);
                    if (langID == -1) {
                        System.err.println("ERROR: language code " + args[i] + " does not occur in database!");
                        System.exit(1);
                    }
                    relevantLangCodes[i - 1] = args[i];
                    relevantLangIDs[i - 1] = langID;
                }
            }

            Map<String, Integer> relevantLangToID = new TreeMap<String, Integer>();
            for (int langID = 0; langID < relevantLangIDs.length; langID++) {
                relevantLangToID.put(relevantLangCodes[langID], relevantLangIDs[langID]);
            }

            for (int conceptID = 0; conceptID < database.getNumConcepts(); conceptID++) {
                List<List<Integer>> formsPerLang = database.getFormIDsForConceptPerLanguage(conceptID);
                for (int langID = 0; langID < database.getNumLanguages(); langID++) {
                    for (int langFormID : formsPerLang.get(langID)) {
                        PhoneticString langForm = database.getForm(langFormID);
                        double infoLength = 0.0;
                        for (int i = 0; i < langForm.getLength(); i++) {
                            infoLength += InformationWeightedSequenceAlignment.getInfoScore(langForm, i,
                                    infoModels[langID]);
                        }
                        System.out.print(database.getConceptName(conceptID) + "\t");
                        System.out.print(database.getLanguageCode(langID) + "\t");
                        System.out.print(database.getAnnotation("Word_Form", langFormID) + "\t");
                        System.out.print(langForm.toString(symbolTable) + "\t");
                        System.out.println(infoLength);
                    }
                }
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
