package de.jdellert.iwsa.sequence;

import de.tuebingen.sfs.cldfjava.data.CLDFForm;
import de.tuebingen.sfs.cldfjava.data.CLDFWordlistDatabase;

import java.io.Serializable;
import java.util.*;

/**
 * Symbol table for mapping IPA segments to integers for efficient internal
 * representation. The first two integers are always used for special symbols:
 * 0 ~ #: the word boundary symbol 1 ~ -: the gap symbol
 */

public class PhoneticSymbolTable implements Serializable {
    public static final int BOUNDARY_ID = 0;
    public static final String BOUNDARY_SYMBOL = "#";
    public static final int EMPTY_ID = 1;
    public static final String EMPTY_SYMBOL = "-";
    public static final String UNKNOWN_SYMBOL = "?";
    private static final long serialVersionUID = -8825447220839372572L;
    protected List<String> idToSymbol;
    protected Map<String, Integer> symbolToID;
    protected int nextID;

    public PhoneticSymbolTable() {
        this.idToSymbol = new ArrayList<>();
        this.symbolToID = new TreeMap<>();
        this.nextID = 0;
    }

    public PhoneticSymbolTable(Collection<String> symbols) {
        this();
        defineSymbols(symbols);
    }

    protected void defineSymbols(Collection<String> symbols) {
        this.idToSymbol = new ArrayList<>(symbols.size() + 2);
        this.symbolToID = new TreeMap<String, Integer>();
        idToSymbol.add("");
        idToSymbol.add("");
        idToSymbol.set(BOUNDARY_ID, BOUNDARY_SYMBOL);
        idToSymbol.set(EMPTY_ID, EMPTY_SYMBOL);
        symbolToID.put(BOUNDARY_SYMBOL, BOUNDARY_ID);
        symbolToID.put(EMPTY_SYMBOL, EMPTY_ID);
        nextID = 2;
        for (String symbol : symbols) {
            defineSymbol(symbol);
        }
    }

    public int defineSymbol(String symbol) {
        if (symbolToID.containsKey(symbol)) {
            System.err.println("WARNING from PhoneticSymbolTable: symbol \"symbol\" is already defined, returning the existing ID.");
            return symbolToID.get(symbol);
        } else {
            idToSymbol.add(symbol);
            symbolToID.put(symbol, nextID);
            nextID++;
            return nextID - 1;
        }
    }

    public boolean contains(String symbol) {
        return symbolToID.containsKey(symbol);
    }

    public Integer toInt(String symbol) {
        return symbolToID.getOrDefault(symbol, -1);
    }

    public String toSymbol(int id) {
        return (id < 0) ? UNKNOWN_SYMBOL : idToSymbol.get(id);
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

    public Set<String> getDefinedSymbols() {
        return new TreeSet<String>(symbolToID.keySet());
    }

    public int getSize() {
        return idToSymbol.size();
    }

    public String toSymbolPair(int symbolPairID) {
        return "(" + toSymbol(symbolPairID / idToSymbol.size()) + "," + toSymbol(symbolPairID % idToSymbol.size()) + ")";
    }

    public String toString() {
        StringBuilder line1 = new StringBuilder();
        StringBuilder line2 = new StringBuilder();
        for (int i = 0; i < idToSymbol.size(); i++) {
            line1.append(i + "\t");
            line2.append(idToSymbol.get(i) + "\t");
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
            return i + 1 < idToSymbol.size();
        }

        @Override
        public String next() {
            if (hasNext())
                i++;
            return idToSymbol.get(i);
        }
    }


    public static PhoneticSymbolTable symbolTableFromDatabase(CLDFWordlistDatabase database) {
        Set<String> usedIpaTokens = new TreeSet<String>();

        Map<Integer, CLDFForm> formsMap = database.getFormsMap();
        for (Map.Entry<Integer, CLDFForm> entry : formsMap.entrySet()) {
            CLDFForm form = entry.getValue();
            String[] segments = form.getSegments();
            usedIpaTokens.addAll(Arrays.asList(segments));
        }

        PhoneticSymbolTable symbolTable = new PhoneticSymbolTable(usedIpaTokens);

        return symbolTable;
    }
}
