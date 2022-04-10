package de.jdellert.iwsa.features;

import java.util.Arrays;

public class DiacriticHandlingTest {
    public static void main(String[] args) {
        try {
            IpaFeatureTable dummyFeatureTable = new IpaFeatureTable("iwsa/src/test/resources/de/jdellert/iwsa/features/test_symbols.csv");
            IpaFeatureTable goldFeatureTable = new IpaFeatureTable(
                    "iwsa/src/main/resources/de/jdellert/iwsa/features/all_ipa_symbols.csv",
                    "iwsa/src/test/resources/de/jdellert/iwsa/features/dummy_modifier_rules.csv"
            );

            String[] testSounds = {"kʼ", "kʰː", "kːʰ", "ⁿk̝ːʰ", "kʰ", "k̝", "k",
                                    "aui", "auːi", "aːui", "auiː", "aũi", "ãũĩ", "ã͡ũĩ"};

            for (String s : testSounds) {
                System.out.println("Sound: " + s);
                System.out.println("Features are equal: " + (Arrays.equals(dummyFeatureTable.get(s), goldFeatureTable.get(s))));
                System.out.println(Arrays.toString(dummyFeatureTable.get(s)));
                System.out.println(Arrays.toString(goldFeatureTable.get(s)));
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
