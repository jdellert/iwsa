package de.jdellert.iwsa.sequence;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GeneralizedPhoneticSymbolTable extends PhoneticSymbolTable {

    private static final Collection<String> symbols = null; //TODO: stub to make this compilable, remove later

    public GeneralizedPhoneticSymbolTable() {
        super();
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
}
