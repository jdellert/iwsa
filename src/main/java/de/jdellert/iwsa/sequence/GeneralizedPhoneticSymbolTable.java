package de.jdellert.iwsa.sequence;

import java.util.Collection;

public class GeneralizedPhoneticSymbolTable extends PhoneticSymbolTable {

    private static final Collection<String> symbols = null; //TODO: stub to make this compilable, remove later

    public GeneralizedPhoneticSymbolTable() {
        //TODO: load symbol sequence from IpaFeatureTable, use this to populate the Phonetic Symbol Table
        //use as replacement to the PhoneticSymbolTable extracted from the corrspondence object in order to canonize PhoneticString representations
        super(symbols);
    }
}
