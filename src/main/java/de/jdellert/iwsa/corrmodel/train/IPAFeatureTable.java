package de.jdellert.iwsa.corrmodel.train;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.DataFormatException;

public class IPAFeatureTable {
    private Map<String, int[]> featureTable;

    public IPAFeatureTable(String filepath) throws DataFormatException, IOException {
        featureTable = new HashMap<>();
        BufferedReader br = new BufferedReader(new FileReader(filepath));
        String line;
        while ((line = br.readLine()) != null) {
            String[] fields = line.split(",");
            if (fields[0].equals("")) {
                continue;
            }
            int[] features = new int[fields.length - 1];
            for (int i = 0; i < features.length; i++) {
                String feature = fields[i+1];
                int featureValue = switch (feature) {
                    case "+" -> 1;
                    case "-" -> -1;
                    case "0" -> 0;
                    default -> throw new DataFormatException();
                };
                features[i] = featureValue;
            }
            featureTable.put(fields[0], features);
        }
        br.close();
    }

    public int[] get(String key) {
        return featureTable.get(key);
    }

    public Map<String, int[]> getFeatureTable() {
        return featureTable;
    }
}
