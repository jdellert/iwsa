package de.jdellert.iwsa.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import de.jdellert.iwsa.sequence.PhoneticString;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;

/**
 * Building a LexicalDatabase object from a TSV file in CLDF format.
 * 
 * @author jdellert
 *
 */

public class CLDFImport {
	public static LexicalDatabase loadDatabase(String fileName) throws IOException {

		List<String> langCodePerLine = new ArrayList<String>();
		Set<String> langCodes = new TreeSet<String>();

		List<String> conceptNamePerLine = new ArrayList<String>();
		Set<String> conceptNames = new TreeSet<String>();

		List<String[]> tokenizedIpaPerLine = new ArrayList<String[]>();
		Set<String> usedIpaTokens = new TreeSet<String>();

		int langCodeColumnIdx = -1;
		int conceptNameColumnIdx = -1;
		int ipaColumnIdx = -1;

		BufferedReader r = new BufferedReader(new FileReader(new File(fileName)));
		String line = r.readLine();
		String[] tokens = line.split("\t");
		for (int columnIdx = 0; columnIdx < tokens.length; columnIdx++) {
			switch (tokens[columnIdx]) {
			case "Language_ID":
				langCodeColumnIdx = columnIdx;
				break;
			case "Concept_ID":
				conceptNameColumnIdx = columnIdx;
				break;
			case "IPA":
				ipaColumnIdx = columnIdx;
				break;
			}
		}
		// TODO: load other column names, store content of these columns as annotations

		if (langCodeColumnIdx == -1) {
			r.close();
			throw new IOException("ERROR: no column \"Language_ID\" found in " + fileName + "!");
		}
		if (conceptNameColumnIdx == -1) {
			r.close();
			throw new IOException("ERROR: no column \"Concept_ID\" found in " + fileName + "!");
		}
		if (ipaColumnIdx == -1) {
			r.close();
			throw new IOException("ERROR: no column \"IPA\" found in " + fileName + "!");
		}
		while ((line = r.readLine()) != null) {
			tokens = line.split("\t");

			String langCode = tokens[langCodeColumnIdx];
			langCodePerLine.add(langCode);
			langCodes.add(langCode);

			String conceptName = tokens[conceptNameColumnIdx];
			conceptNamePerLine.add(conceptName);
			conceptNames.add(conceptName);

			String[] tokenizedIPA = tokens[ipaColumnIdx].split(" ");
			tokenizedIpaPerLine.add(tokenizedIPA);
			for (String ipaToken : tokenizedIPA) {
				usedIpaTokens.add(ipaToken);
			}
		}
		r.close();

		PhoneticSymbolTable symbolTable = new PhoneticSymbolTable(usedIpaTokens);
		String[] langCodesArray = langCodes.toArray(new String[langCodes.size()]);
		String[] conceptNamesArray = conceptNames.toArray(new String[conceptNames.size()]);

		System.out.print("Building database using " + symbolTable.getSize() + " symbols for " + langCodePerLine.size()
				+ " forms covering " + conceptNamesArray.length + " concepts across " + langCodesArray.length
				+ " languages ... ");
		LexicalDatabase database = new LexicalDatabase(symbolTable, langCodesArray, conceptNamesArray,
				langCodePerLine.size());
		for (int lineNumber = 0; lineNumber < langCodePerLine.size(); lineNumber++) {
			database.addForm(langCodePerLine.get(lineNumber), conceptNamePerLine.get(lineNumber),
					new PhoneticString(symbolTable.encode(tokenizedIpaPerLine.get(lineNumber))));
		}
		System.out.println("done.");

		return database;
	}
}
