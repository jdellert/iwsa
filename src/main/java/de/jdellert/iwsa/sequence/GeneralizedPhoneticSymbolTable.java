package de.jdellert.iwsa.sequence;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class GeneralizedPhoneticSymbolTable extends PhoneticSymbolTable {

    private Map<Integer, List<Integer>> combinedSymbols;
    private Map<Integer, Set<Integer>> metasymbols;
    private static final String IPA_FILE_PATH = "features-all_ipa_symbols.csv";

    public GeneralizedPhoneticSymbolTable() {
        super();
        this.combinedSymbols = new HashMap<>();
        this.metasymbols = new HashMap<>();
        List<String> symbolList = loadDefaultSymbolList();
        defineSymbols(symbolList);
    }

    private List<String> loadDefaultSymbolList() {
        List<String> symbols = new ArrayList<>();
        try(InputStream rawInputStream = ClassLoader.getSystemResourceAsStream(IPA_FILE_PATH);
            BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(rawInputStream,"File "+IPA_FILE_PATH+" not found!"), StandardCharsets.UTF_8))) {
            for(String line; (line = reader.readLine()) != null;) {
                String[] fields = line.split(",");
                if (fields[0].equals("")) {
                    continue;
                }
                symbols.add(fields[0]);
            }
        } catch (IOException e) {
            System.err.println("ERROR: IO exception while reading in from " + IPA_FILE_PATH);
            e.printStackTrace();
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
