package de.jdellert.iwsa;

import de.jdellert.iwsa.data.CLDFImport;
import de.jdellert.iwsa.data.LexicalDatabase;
import de.jdellert.iwsa.infomodel.InformationModel;
import de.jdellert.iwsa.infomodel.InformationModelInference;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;

import java.io.IOException;

/**
 * A class for inspecting language-specific information models. The output
 * gives the observed counts of trigrams organized by gappy bigrams.
 */

public class InformationModelOutput {
    public static void main(String[] args) {
        try {
            LexicalDatabase database = CLDFImport.loadDatabase(args[0], true);
            PhoneticSymbolTable symbolTable = database.getSymbolTable();

            String langCode = args[1];
            int langID = database.getIDForLanguageCode(langCode);
            if (langID == -1) {
                System.err.println("ERROR: language code " + langCode + " not represented in database!");
                System.exit(1);
            }

            InformationModel model = InformationModelInference.inferInformationModelForLanguage(langID, database, symbolTable);
            model.printCounts(System.out, false);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
