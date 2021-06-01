package de.jdellert.iwsa.data;

import java.io.FileWriter;
import java.io.IOException;

public class CLDFExport {

    public static void exportToFile(LexicalDatabase database, String fileName, boolean header) throws IOException {
        FileWriter out = new FileWriter(fileName);
        for (int formID = 0; formID < database.getNumForms(); formID++) {
            String conceptForForm = database.getConceptNameForForm(formID);
            String languageForForm = database.getLanguageCodeForForm(formID);

            String orthForm = database.getAnnotation("Word_Form", formID);
            String ipaForm = database.getForm(formID).toString(database.getSymbolTable());

            int cognateSetID = database.getCognateSetID(formID);

            out.write(conceptForForm + "\t" + languageForForm + "\t" + orthForm + "\t" + ipaForm + "\t" + cognateSetID + "\n");
        }
        out.close();
    }

}
