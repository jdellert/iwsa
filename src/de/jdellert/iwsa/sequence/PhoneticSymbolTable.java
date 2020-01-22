package de.jdellert.iwsa.sequence;

import java.io.Serializable;
import java.util.*;

/**
 * Symbol table for mapping IPA segments to integers for efficient internal
 * representation. The first two integers are always used for special symbols: 0
 * ~ #: the word boundary symbol 1 ~ -: the gap symbol
 *
 */

public class PhoneticSymbolTable implements Serializable {
    private static final long serialVersionUID = -8825447220839372572L;

    public static final int BOUNDARY_ID = 0;
    public static final String BOUNDARY_SYMBOL = "#";
    public static final int EMPTY_ID = 1;
    public static final String EMPTY_SYMBOL = "-";

    public static final String UNKNOWN_SYMBOL = "/";

    private String[] idToSymbol;
    private Map<String, Integer> symbolToID;

    public PhoneticSymbolTable(Collection<String> symbols) {
        this.idToSymbol = new String[symbols.size() + 2];
        this.symbolToID = new TreeMap<String, Integer>();
        idToSymbol[BOUNDARY_ID] = BOUNDARY_SYMBOL;
        idToSymbol[EMPTY_ID] = EMPTY_SYMBOL;
        symbolToID.put(BOUNDARY_SYMBOL, BOUNDARY_ID);
        symbolToID.put(EMPTY_SYMBOL, EMPTY_ID);
        int nextID = 2;
        for (String symbol : symbols) {
            idToSymbol[nextID] = symbol;
            symbolToID.put(symbol, nextID);
            nextID++;
        }
    }

    public boolean contains(String symbol) {
        return symbolToID.containsKey(symbol);
    }

    public Integer toInt(String symbol) {
        return symbolToID.getOrDefault(symbol, -1);
    }

    public String toSymbol(int id) {
        return (id < 0) ? UNKNOWN_SYMBOL : idToSymbol[id];
    }

    public int[] encode(String[] segments) {
        int[] segmentIDs = new int[segments.length];
        for (int idx = 0; idx < segments.length; idx++) {
            segmentIDs[idx] = toInt(segments[idx]);
        }
        return segmentIDs;
    }

    public String[] decode(int[] segmentIDs) {
        String[] segments = new String[segmentIDs.length];
        for (int idx = 0; idx < segmentIDs.length; idx++) {
            segments[idx] = toSymbol(segmentIDs[idx]);
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

    public Iterator<String> iterator() {
        return new PhoneticSymbolTableIterator();
    }

    private class PhoneticSymbolTableIterator implements Iterator<String> {

        private int i;

        public PhoneticSymbolTableIterator() {
            i = 1;
        }

        @Override
        public boolean hasNext() {
            return i+1 < idToSymbol.length;
        }

        @Override
        public String next() {
            if (hasNext())
                i++;
            return idToSymbol[i];
        }
    }
}
