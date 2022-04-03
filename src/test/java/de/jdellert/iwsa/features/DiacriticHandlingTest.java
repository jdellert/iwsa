package de.jdellert.iwsa.features;

import java.util.Arrays;

public class DiacriticHandlingTest {
    public static void main(String[] args) {
        try {
            IpaFeatureTable featureTable = new IpaFeatureTable("iwsa/src/test/resources/de/jdellert/iwsa/features/test_symbols.csv");
            // ˤ ː ʰ
            System.out.println(Arrays.toString(featureTable.get("k")));
            System.out.println(Arrays.toString(featureTable.get("kˤ")));
            System.out.println(Arrays.toString(featureTable.get("kˤː")));
            System.out.println(Arrays.toString(featureTable.get("kːˤ")));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
