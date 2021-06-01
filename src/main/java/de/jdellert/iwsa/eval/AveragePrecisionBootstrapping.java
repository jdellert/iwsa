package de.jdellert.iwsa.eval;

import de.jdellert.iwsa.util.io.SimpleFormatReader;
import de.jdellert.iwsa.util.ranking.RankingEntry;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class AveragePrecisionBootstrapping {
    /**
     * Processes a cognacy gold standard file, and derives average precision with confidence
     * intervals for each of the methods via bootstrapping.
     */
    public static void main(String[] args) {
        // for cognacy evaluation files: startIndex = 9
        if (args.length < 2) {
            System.err.println("Usage: AveragePrecisionBootstrapping [gold standard file with distance values] [startIndex]");
            System.exit(0);
        }

        //three variants: all pairs, only pairs from same genus, only pairs from different genera
        Map<String, String> genusForLang = new TreeMap<String, String>();
        genusForLang.put("deu", "Germanic");
        genusForLang.put("eng", "Germanic");
        genusForLang.put("nld", "Germanic");
        genusForLang.put("dan", "Germanic");
        genusForLang.put("swe", "Germanic");
        genusForLang.put("nor", "Germanic");
        genusForLang.put("isl", "Germanic");
        genusForLang.put("fra", "Romance");
        genusForLang.put("spa", "Romance");
        genusForLang.put("ita", "Romance");
        genusForLang.put("cat", "Romance");
        genusForLang.put("por", "Romance");
        genusForLang.put("ron", "Romance");
        genusForLang.put("lat", "Romance");
        genusForLang.put("cym", "Celtic");
        genusForLang.put("gle", "Celtic");
        genusForLang.put("bre", "Celtic");
        genusForLang.put("lav", "Baltic");
        genusForLang.put("lit", "Baltic");
        genusForLang.put("rus", "Slavic");
        genusForLang.put("ukr", "Slavic");
        genusForLang.put("bel", "Slavic");
        genusForLang.put("pol", "Slavic");
        genusForLang.put("ces", "Slavic");
        genusForLang.put("slk", "Slavic");
        genusForLang.put("slv", "Slavic");
        genusForLang.put("hrv", "Slavic");
        genusForLang.put("bul", "Slavic");
        genusForLang.put("hye", "Armenian");
        genusForLang.put("sqi", "Albanian");
        genusForLang.put("ell", "Hellenic");
        genusForLang.put("oss", "Iranian");
        genusForLang.put("pes", "Iranian");
        genusForLang.put("pbu", "Iranian");
        genusForLang.put("kur", "Iranian");
        genusForLang.put("hin", "Indo-Aryan");
        genusForLang.put("ben", "Indo-Aryan");

        // for cognacy: min precision = 0.9
        // minPrecision = 0.9;

        double minPrecision = 0.0;

        try {
            // load the gold standard pairs
            String goldStandardFileName = args[0];
            List<String[]> lines = SimpleFormatReader.arrayFromTSV(goldStandardFileName);

            // start index determining where the values start
            int startIndex = Integer.parseInt(args[1]);

            System.out.println("variant\taveP [aveP-, aveP+]");

            String[] columnNames = lines.remove(0);
            for (int i = startIndex + 1; i < columnNames.length; i++) {
                String variantName = columnNames[i];


                List<Double> avgPrecisionValues = new ArrayList<Double>(1000);
                //bootstrap samples
                for (int sampleID = 1; sampleID <= 1000; sampleID++) {
                    // sort resulting list of distances in ascending order
                    int numCognates = 0;
                    List<RankingEntry<Boolean>> distancesRankingNonCognate = new ArrayList<RankingEntry<Boolean>>();
                    List<RankingEntry<Boolean>> distancesRankingCognate = new ArrayList<RankingEntry<Boolean>>();

                    for (int j = 0; j < lines.size(); j++) {
                        String[] randomLine = lines.get((int) (lines.size() * Math.random()));
                        String lang1 = randomLine[1];
                        String lang2 = randomLine[5];
                        if (genusForLang.get(lang1) == null || genusForLang.get(lang2) == null) {
                            System.err.println("ERROR: no genus defined for a from pair (" + lang1 + ", " + lang2 + ")");
                            System.exit(1);
                        }
                        boolean sameGenus = genusForLang.get(lang1).equals(genusForLang.get(lang2));
                        if (sameGenus) continue;

                        boolean cognate = randomLine[startIndex].equals("T") ? true : false;
                        Double distanceValue = Double.parseDouble(randomLine[i]);
                        if (cognate) {
                            numCognates++;
                            distancesRankingCognate.add(new RankingEntry<Boolean>(cognate, distanceValue));
                        } else {
                            distancesRankingNonCognate.add(new RankingEntry<Boolean>(cognate, distanceValue));
                        }

                    }

                    List<RankingEntry<Boolean>> distancesRanking = distancesRankingNonCognate;
                    distancesRanking.addAll(distancesRankingCognate);

                    //stable sort guaranteed => false instances will come first!
                    Collections.sort(distancesRanking);

                    // evaluate precision and recall for the top-k elements in each list (i.e.
                    // assuming that the threshold is at k-th value)
                    double bestFscore = 0.0;
                    double bestPrecision = 0.0;
                    double bestRecall = 0.0;
                    double bestThresh = 0.0;
                    double lastRecallThreshold = 0.000;

                    double averagePrecision = 0.0;
                    double tp = 0.0;
                    double tn = distancesRanking.size() - numCognates + 0.0;
                    double fp = 0.0;
                    double fn = numCognates + 0.0;
                    for (int k = 0; k < distancesRanking.size(); k++) {
                        RankingEntry<Boolean> entry = distancesRanking.get(k);
                        if (entry.key == true) {
                            tp++;
                            fn--;
                        } else {
                            fp++;
                            tn--;
                        }
                        double precision = tp / (tp + fp);
                        double recall = tp / (tp + fn);
                        double fscore = 2 * precision * recall / (precision + recall);

                        if (precision >= minPrecision && fscore > bestFscore) {
                            bestFscore = fscore;
                            bestPrecision = precision;
                            bestRecall = recall;
                            bestThresh = entry.value;
                        }

                        // while doing this, also compute the average precision for each method
                        while (recall > lastRecallThreshold) {
                            averagePrecision += precision * 0.001;
                            lastRecallThreshold += 0.001;
                        }
                    }
                    avgPrecisionValues.add(averagePrecision);

                }
                Collections.sort(avgPrecisionValues);
                double averageAvgPrecision = avgPrecisionValues.stream().mapToDouble(val -> val).average().getAsDouble();
                double lowerAvgPrecision = avgPrecisionValues.get(25);
                double upperAvgPrecision = avgPrecisionValues.get(975);

                System.out.println(variantName + "\t"
                        + String.format("%.3f", averageAvgPrecision).replace(",", ".") + " ["
                        + String.format("%.3f", lowerAvgPrecision).replace(",", ".") + ", "
                        + String.format("%.3f", upperAvgPrecision).replace(",", ".") + "]");
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
