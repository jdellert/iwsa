package de.jdellert.iwsa.sequence;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class GeneralizedPhoneticSymbolTable extends PhoneticSymbolTable {

    private Map<Integer, List<Integer>> combinedSymbols;
    private Map<Integer, Set<Integer>> metasymbols;

    public GeneralizedPhoneticSymbolTable() {
        super();
        this.combinedSymbols = new HashMap<>();
        this.metasymbols = new HashMap<>();
        List<String> symbolList = loadDefaultSymbolList();
        defineSymbols(symbolList);
    }

    private List<String> loadDefaultSymbolList() {
        List<String> symbols = new ArrayList<String>();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    this.getClass().getResourceAsStream("/de/jdellert/iwsa/features/all_ipa_symbols.csv")));
            String line;
            while ((line = br.readLine()) != null) {
                String[] fields = line.split(",");
                if (fields[0].equals("")) {
                    continue;
                }
                symbols.add(fields[0]);
            }
            br.close();
        }
        catch (IOException e) {
            System.err.println("ERROR: IO exception while reading in from all_ipa_symbols.csv");
        }
        return symbols;
    }

    public List<Integer> getCombinedSymbolComponentsOrNull(int combinedSymbolId) {
        return combinedSymbols.get(combinedSymbolId);
    }

    public Set<Integer> getMetasymbolExtensionsOrNull(int metasymbolId) {
        return metasymbols.get(metasymbolId);
    }

    public void declareCombinedSymbol(String combinedSymbol, String ... symbols) {
        if (toInt(combinedSymbol) != -1) {
            System.err.println("WARNING from GeneralizedPhoneticSymbolTable: ignoring declaration of combined symbol \"" + combinedSymbol + "\" that already exists!");
            return;
        }
        List<Integer> componentIds = new ArrayList<>(symbols.length);
        for (String component : symbols) {
            int componentId = toInt(component);
            if (componentId == -1) {
                System.err.println("ERROR in GeneralizedPhoneticSymbolTable: attempting to declare a combined symbol \"" + combinedSymbol + "\" using the undeclared symbol \"" + component + "\"");
                return;
            }
            componentIds.add(componentId);
        }
        idToSymbol.add(combinedSymbol);
        symbolToID.put(combinedSymbol, nextID);
        combinedSymbols.put(nextID, componentIds);
        nextID++;
    }

    public void declareMetasymbol(String metasymbol, String ... symbols) {
        if (toInt(metasymbol) != -1) {
            System.err.println("WARNING from GeneralizedPhoneticSymbolTable: ignoring declaration of metasymbol \"" + metasymbol + "\" that already exists!");
            return;
        }
        Set<Integer> extensionIds = new HashSet<>(symbols.length);
        for (String extension : symbols) {
            int extensionId = toInt(extension);
            if (extensionId == -1) {
                System.err.println("ERROR in GeneralizedPhoneticSymbolTable: attempting to declare a metasymbol \"" + metasymbol + "\" using the undeclared symbol \"" + extension + "\"");
                return;
            }
            extensionIds.add(extensionId);
        }
        idToSymbol.add(metasymbol);
        symbolToID.put(metasymbol, nextID);
        metasymbols.put(nextID, extensionIds);
        nextID++;
    }

}
