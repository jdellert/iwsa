package de.jdellert.iwsa.sequence;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Symbol table for mapping IPA segments to integers for efficient internal
 * representation. The first two integers are always used for special symbols: 0
 * ~ #: the word boundary symbol 1 ~ -: the gap symbol
 *
 */

public class PhoneticSymbolTable implements Serializable {
	private static final long serialVersionUID = -8825447220839372572L;
	
	private String[] idToSymbol;
	private Map<String, Integer> symbolToID;

	public PhoneticSymbolTable(Collection<String> symbols) {
		this.idToSymbol = new String[symbols.size() + 2];
		this.symbolToID = new TreeMap<String, Integer>();
		idToSymbol[0] = "#";
		idToSymbol[1] = "-";
		symbolToID.put("#", 0);
		symbolToID.put("-", 1);
		int nextID = 2;
		for (String symbol : symbols) {
			idToSymbol[nextID] = symbol;
			symbolToID.put(symbol, nextID);
			nextID++;
		}
	}

	public Integer toInt(String symbol) {
		return symbolToID.get(symbol);
	}

	public String toSymbol(int id) {
		return idToSymbol[id];
	}

	public int[] encode(String[] segments) {
		int[] segmentIDs = new int[segments.length];
		for (int idx = 0; idx < segments.length; idx++) {
			segmentIDs[idx] = symbolToID.get(segments[idx]);
		}
		return segmentIDs;
	}

	public String[] decode(int[] segmentIDs) {
		String[] segments = new String[segmentIDs.length];
		for (int idx = 0; idx < segmentIDs.length; idx++) {
			segments[idx] = idToSymbol[segmentIDs[idx]];
		}
		return segments;
	}
	
	public Set<String> getDefinedSymbols()
	{
		return new TreeSet<String>(symbolToID.keySet());
	}

	public int getSize() {
		return idToSymbol.length;
	}

	public String toSymbolPair(int symbolPairID) {
		return "(" + toSymbol(symbolPairID / idToSymbol.length) + "," + toSymbol(symbolPairID % idToSymbol.length) + ")";
	}
	
	public String toString()
	{
		StringBuilder line1 = new StringBuilder();
		StringBuilder line2 = new StringBuilder();
		for (int i = 0; i < idToSymbol.length; i++)
		{
			line1.append(i + "\t");
			line2.append(idToSymbol[i] + "\t");
		}
		return line1 + "\n" + line2;
	}
}
