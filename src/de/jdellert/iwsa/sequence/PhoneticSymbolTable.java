package de.jdellert.iwsa.sequence;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * Symbol table for mapping IPA segments to integers for efficient internal
 * representation.
 * 
 * @author jdellert
 *
 */

public class PhoneticSymbolTable {
	private String[] idToSymbol;
	private Map<String, Integer> symbolToID;

	public PhoneticSymbolTable(Collection<String> symbols) {
		this.idToSymbol = symbols.toArray(new String[symbols.size()]);
		this.symbolToID = new TreeMap<String, Integer>();
		for (int symbolID = 0; symbolID < idToSymbol.length; symbolID++) {
			symbolToID.put(idToSymbol[symbolID], symbolID);
		}
	}

	public int toInt(String symbol) {
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

	public int getSize() {
		return idToSymbol.length;
	}
}
