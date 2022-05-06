package de.jdellert.iwsa.sequence.ipa;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * This class provides an ordered list of common IPA symbols along with their
 * definitions, and various helper methods for output of IPA symbols e.g. in
 * TIPA for LaTeX
 *
 */

public class IpaSymbolInformation {
	private static List<String> defaultSymbolOrdering;
	private static Map<String, String> symbolDescriptions;
	private static Map<String, String> ipaToTipa;

	private static final String IPA_FILE1 = "ipa-alphabet-descriptions.tsv";
	private static final String IPA_FILE2 = "ipa-tipa-equivalents.tsv";

	static {
		defaultSymbolOrdering = new ArrayList<>();
		symbolDescriptions = new TreeMap<>();
		ipaToTipa = new TreeMap<>();

		try(InputStream inStream = IpaSymbolInformation.class.getResourceAsStream(IPA_FILE1);
			BufferedReader in = new BufferedReader(new InputStreamReader(Objects.requireNonNull(inStream, "File "+IPA_FILE1+" not found!"), StandardCharsets.UTF_8))) {
			for(String line; (line = in.readLine()) != null;) {
				String[] symbolAndDescription = line.split("\t");
				String symbol = symbolAndDescription[0];
				defaultSymbolOrdering.add(symbol);
				symbolDescriptions.put(symbol, symbolAndDescription[1]);
			}
		} catch (IOException e) {
			System.err.println("ERROR: IO exception while reading in from " + IPA_FILE1);
			e.printStackTrace();
		}

		try(InputStream rawInputStream = IpaSymbolInformation.class.getResourceAsStream(IPA_FILE2);
			BufferedReader in = new BufferedReader(new InputStreamReader(Objects.requireNonNull(rawInputStream,"File "+IPA_FILE2+" not found!"), StandardCharsets.UTF_8))) {
			for(String line; (line = in.readLine()) != null;) {
				String[] ipaAndTipa = line.split("\t");
				ipaToTipa.put(ipaAndTipa[0], ipaAndTipa[1]);
			}
		} catch (IOException e) {
			System.err.println("ERROR: IO exception while reading in from " + IPA_FILE2);
			e.printStackTrace();
		}
	}

	public static List<String> getKnownSymbolsInOrder() {
		return defaultSymbolOrdering;
	}

	public static String getDescriptionForSymbol(String symbol) {
		String description = symbolDescriptions.get(symbol);
		if (description == null) {
			description = "unknown symbol";
		}
		return description;
	}

	public static String getTipaForSymbol(String symbol) {
		String tipa = ipaToTipa.get(symbol);
		if (tipa == null) {
			if (symbol.length() > 1) {
				tipa = "";
				for (String elementarySymbol : symbol.split("")) {
					tipa += getTipaForSymbol(elementarySymbol);
				}
			} else {
				tipa = symbol;
			}
		}
		return tipa;
	}
}
