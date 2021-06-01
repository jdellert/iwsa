package de.jdellert.iwsa.eval;

import de.jdellert.iwsa.util.io.SimpleFormatReader;
import de.jdellert.iwsa.util.ranking.RankingEntry;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PrecisionRecallCurveOutput {
    /**
     * Processes a cognacy gold standard file, and adds the values of various
     * distances to a copy of the file as output. Assumes that correspondence and
     * information models are inferred and accessible for each language (pair).
     */
    public static void main(String[] args) {
        // for cognacy evaluation files: startIndex = 9
        if (args.length < 2) {
            System.err.println("Usage: PrecisionRecallCurves [gold standard file with distance values] [startIndex]");
            System.exit(0);
        }

        // for cognacy: min precision = 0.9
        // minPrecision = 0.9;

        double minPrecision = 0.0;

        try {
            // load the gold standard pairs
            String goldStandardFileName = args[0];
            List<String[]> lines = SimpleFormatReader.arrayFromTSV(goldStandardFileName);

            // start index determining where the values start
            int startIndex = Integer.parseInt(args[1]);

            System.out.println("variant\taveP\tFsco\tPrec\tReca\tThre");

            String[] columnNames = lines.remove(0);
            for (int i = startIndex + 1; i < columnNames.length; i++) {
                String variantName = columnNames[i];

                String outputFileName = goldStandardFileName.replace(".tsv", "") + "-" + variantName + ".tsv";
                FileWriter out = new FileWriter(new File(outputFileName));
                out.write("value\trecall\tprecision\n");

                // sort resulting list of distances in ascending order
                int numCognates = 0;
                List<RankingEntry<Boolean>> distancesRanking = new ArrayList<RankingEntry<Boolean>>();
                //first the non-cognates (worst possible result if distances are tied)
                for (String[] line : lines) {
                    boolean cognate = line[startIndex].equals("T") ? true : false;
                    if (!cognate) {
                        Double distanceValue = Double.parseDouble(line[i]);
                        distancesRanking.add(new RankingEntry<Boolean>(cognate, distanceValue));
                    }
                }
                for (String[] line : lines) {
                    boolean cognate = line[startIndex].equals("T") ? true : false;
                    if (cognate) {
                        numCognates++;
                        Double distanceValue = Double.parseDouble(line[i]);
                        distancesRanking.add(new RankingEntry<Boolean>(cognate, distanceValue));
                    }
                }
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

                    // output the precision and recall values for the precision-recall curves
                    out.write(String.format("%.8f", entry.value).replace(",", ".") + "\t"
                            + String.format("%.8f", recall).replace(",", ".") + "\t"
                            + String.format("%.8f", precision).replace(",", ".") + "\n");

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
                System.out.println(variantName + "\t" + String.format("%.3f", averagePrecision).replace(",", ".") + "\t"
                        + String.format("%.3f", bestFscore).replace(",", ".") + "\t"
                        + String.format("%.3f", bestPrecision).replace(",", ".") + "\t"
                        + String.format("%.3f", bestRecall).replace(",", ".") + "\t"
                        + String.format("%.3f", bestThresh).replace(",", "."));
                out.close();
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
