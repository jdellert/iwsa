package de.jdellert.iwsa.data;

import de.jdellert.iwsa.sequence.PhoneticString;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;
import de.jdellert.iwsa.tokenize.IPATokenizer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Building a LexicalDatabase object from a TSV file in CLDF format (extensible, tuned towards NorthEuraLex for now).
 */

public class CLDFImport {
    public static boolean VERBOSE = true;

    public static LexicalDatabase loadDatabase(String fileName, boolean retokenize) throws IOException {
        return loadDatabase(fileName, (retokenize) ? new IPATokenizer() : null);
    }

    public static LexicalDatabase loadDatabase(String fileName, IPATokenizer tokenizer) throws IOException {
        boolean retokenize = tokenizer != null;

        List<String> langCodePerLine = new ArrayList<String>();
        Set<String> langCodes = new TreeSet<String>();

        List<String> conceptNamePerLine = new ArrayList<String>();
        Set<String> conceptNames = new TreeSet<String>();

        List<String[]> tokenizedIpaPerLine = new ArrayList<String[]>();
        Set<String> usedIpaTokens = new TreeSet<String>();

        int langCodeColumnIdx = -1;
        int conceptNameColumnIdx = -1;
        int ipaColumnIdx = -1;
        int segmentColumnIdx = -1;

        List<Map<String, String>> annotationsPerLine = new ArrayList<Map<String, String>>();
        Map<Integer, String> annotationColumns = new TreeMap<Integer, String>();

        BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), StandardCharsets.UTF_8));
        String line = r.readLine();
        String[] tokens = line.split(",");
        for (int columnIdx = 0; columnIdx < tokens.length; columnIdx++) {
            switch (tokens[columnIdx]) {
                case "Language_ID":
                    langCodeColumnIdx = columnIdx;
                    break;
                case "Parameter_ID":
                    conceptNameColumnIdx = columnIdx;
                    break;
                case "Form":
                    ipaColumnIdx = columnIdx;
                    break;
                case "Segments":
                    segmentColumnIdx = columnIdx;
                    break;
                default:
                    annotationColumns.put(columnIdx, tokens[columnIdx]);
            }
        }
        // TODO: load other column names, store content of these columns as annotations

        if (langCodeColumnIdx == -1) {
            r.close();
            throw new IOException("ERROR: no column \"Language_ID\" found in " + fileName + "!");
        }
        if (conceptNameColumnIdx == -1) {
            r.close();
            throw new IOException("ERROR: no column \"Parameter_ID\" found in " + fileName + "!");
        }
        if (ipaColumnIdx == -1) {
            r.close();
            throw new IOException("ERROR: no column \"Form\" found in " + fileName + "!");
        }
        if (segmentColumnIdx == -1) {
            r.close();
            throw new IOException("ERROR: no column \"Segments\" found in " + fileName + "!");
        }
        Set<String> multisegment = new HashSet<>();//TODO
        while ((line = r.readLine()) != null) {
            tokens = line.split(",");

            String langCode = tokens[langCodeColumnIdx];
            langCodePerLine.add(langCode);
            langCodes.add(langCode);

            String conceptName = tokens[conceptNameColumnIdx];
            conceptNamePerLine.add(conceptName);
            conceptNames.add(conceptName);

            String[] tokenizedIPA = null;
            if (retokenize) {
                tokenizedIPA = tokenizer.tokenizeIPA(tokens[ipaColumnIdx]);
            } else {
                tokenizedIPA = tokens[segmentColumnIdx].split(" ");
            }
            tokenizedIpaPerLine.add(tokenizedIPA);
            for (String ipaToken : tokenizedIPA) {
                usedIpaTokens.add(ipaToken);
            }

            Map<String, String> annotations = new TreeMap<String, String>();
            for (Entry<Integer, String> columnAnnotation : annotationColumns.entrySet()) {
                annotations.put(columnAnnotation.getValue(), tokens[columnAnnotation.getKey()]);
            }
            annotationsPerLine.add(annotations);
        }
        r.close();

        PhoneticSymbolTable symbolTable = new PhoneticSymbolTable(usedIpaTokens);
        String[] langCodesArray = langCodes.toArray(new String[langCodes.size()]);
        String[] conceptNamesArray = conceptNames.toArray(new String[conceptNames.size()]);

        if (VERBOSE)
            System.out.print("Building database using " + symbolTable.getSize() + " symbols for " + langCodePerLine.size()
                    + " forms covering " + conceptNamesArray.length + " concepts across " + langCodesArray.length
                    + " languages ... ");
        LexicalDatabase database = new LexicalDatabase(symbolTable, langCodesArray, conceptNamesArray,
                langCodePerLine.size());
        for (int lineNumber = 0; lineNumber < langCodePerLine.size(); lineNumber++) {
            int formID = database.addForm(langCodePerLine.get(lineNumber), conceptNamePerLine.get(lineNumber),
                    new PhoneticString(symbolTable.encode(tokenizedIpaPerLine.get(lineNumber))));
            for (Entry<String, String> annotation : annotationsPerLine.get(lineNumber).entrySet()) {
                database.addAnnotation(formID, annotation.getKey(), annotation.getValue());
            }
        }
        if (VERBOSE) System.out.println("done.");

        return database;
    }
}
