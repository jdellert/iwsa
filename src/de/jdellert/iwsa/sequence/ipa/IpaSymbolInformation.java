package de.jdellert.iwsa.sequence.ipa;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

/**
 * This class provides an ordered list of common IPA symbols along with their
 * definitions, and various helper methods for output of IPA symbols e.g. in
 * TIPA for LaTeX
 * 
 * @author jdellert
 *
 */

public class IpaSymbolInformation {
	private static List<String> defaultSymbolOrdering;
	private static Map<String, String> symbolDescriptions;
	private static Map<String, String> ipaToTipa;

	static {
		defaultSymbolOrdering = new ArrayList<String>();
		symbolDescriptions = new TreeMap<String, String>();
		ipaToTipa = new TreeMap<String, String>();

		InputStream inStream = IpaSymbolInformation.class.getResourceAsStream("ipa-alphabet-descriptions.tsv");
		Scanner in = new Scanner(inStream);
		while (in.hasNextLine()) {
			String[] symbolAndDescription = in.nextLine().split("\t");
			String symbol = symbolAndDescription[0];
			defaultSymbolOrdering.add(symbol);
			symbolDescriptions.put(symbol, symbolAndDescription[1]);
		}
		in.close();

		inStream = IpaSymbolInformation.class.getResourceAsStream("ipa-tipa-equivalents.tsv");
		in = new Scanner(inStream);
		while (in.hasNextLine()) {
			String[] ipaAndTipa = in.nextLine().split("\t");
			ipaToTipa.put(ipaAndTipa[0], ipaAndTipa[1]);
		}
		in.close();

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
