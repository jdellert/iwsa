package de.jdellert.iwsa.corrmodel.train;


import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.ml.Pipeline;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.PipelineStage;
import org.apache.spark.ml.evaluation.RegressionEvaluator;
import org.apache.spark.ml.feature.VectorIndexer;
import org.apache.spark.ml.feature.VectorIndexerModel;
import org.apache.spark.ml.regression.DecisionTreeRegressionModel;
import org.apache.spark.ml.regression.DecisionTreeRegressor;
import org.apache.spark.mllib.regression.LabeledPoint;
import org.apache.spark.mllib.tree.DecisionTree;
import org.apache.spark.mllib.tree.model.DecisionTreeModel;
import org.apache.spark.mllib.util.MLUtils;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.mllib.linalg.DenseVector;
import scala.Tuple2;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.DataFormatException;

public class GeneralizedCorrModelTrainer {
    IPAFeatureTable featureTable;
    String pathToTrainingData;
    String pathToLibSVMData;
    SparkConf sparkConf;
    JavaSparkContext jsc;

    public GeneralizedCorrModelTrainer(IPAFeatureTable featureTable, String pathToTrainingData, String pathToLibSVMData) {
        this.featureTable = featureTable;
        this.pathToTrainingData = pathToTrainingData;
        this.pathToLibSVMData = pathToLibSVMData;
        sparkConf = new SparkConf().setAppName("FeatureGeneralizationsDecisionTree").setMaster("local[2]");
        jsc = new JavaSparkContext(sparkConf);
    }

    public void createLibSVMData(int lowerPMIFilterBoundary, int upperPMIFilterBoundary) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(pathToTrainingData));
        Writer writer = new FileWriter(pathToLibSVMData);
        while (br.readLine() != null) {
            String[] fields = br.readLine().split("\t");
            double score = Double.parseDouble(fields[2]);
            if (lowerPMIFilterBoundary < score && score < upperPMIFilterBoundary) {
                continue;
            }
            writer.write(String.valueOf(score));

            int[] soundPairFeatures = featureTable.getPair(fields[0], fields[1]);

            if (soundPairFeatures == null) {
                continue;
            }

            for (int i = 0; i < soundPairFeatures.length; i++) {
                if (soundPairFeatures[i] != 0) {
                    writer.write(" " + (i+1) + ":" + soundPairFeatures[i]);
                }
            }

            writer.write("\n");
        }
        br.close();
        writer.close();
    }

    public DecisionTreeModel train() {
        return train(null);
    }

    public DecisionTreeModel train(String pathToSaveFile) {
        JavaRDD<LabeledPoint> data = MLUtils.loadLibSVMFile(jsc.sc(), pathToLibSVMData).toJavaRDD();
        JavaRDD<LabeledPoint>[] splits = data.randomSplit(new double[]{0.8, 0.2});
        JavaRDD<LabeledPoint> trainingData = splits[0];
        JavaRDD<LabeledPoint> testData = splits[1];

        Map<Integer, Integer> categoricalFeaturesInfo = new HashMap<>();
        String impurity = "variance";
        int maxDepth = 5;
        int maxBins = 32;

        DecisionTreeModel model = DecisionTree.trainRegressor(trainingData,
                categoricalFeaturesInfo, impurity, maxDepth, maxBins);

        JavaPairRDD<Double, Double> predictionAndLabel =
                testData.mapToPair(p -> new Tuple2<>(model.predict(p.features()), p.label()));
        double testMSE = predictionAndLabel.mapToDouble(pl -> {
            double diff = pl._1() - pl._2();
            return diff * diff;
        }).mean();
        System.out.println("Test Mean Squared Error: " + testMSE);
        System.out.println("Learned regression tree model:\n" + model.toDebugString());

        if (pathToSaveFile != null) {
            model.save(jsc.sc(), pathToSaveFile);
        }

        return model;
    }

    public DecisionTreeModel loadModel(String filePath) {
        return DecisionTreeModel.load(jsc.sc(), filePath);
    }


    public static void main(String[] args) {
        String pathToTrainingData = "src/main/java/de/jdellert/iwsa/corrmodel/train/global-corr-model-output.tsv";
        String pathToLibSVMData = "src/main/java/de/jdellert/iwsa/corrmodel/train/libsvm-corr-data.txt";
        try {
            IPAFeatureTable featureTable = new IPAFeatureTable("src/main/java/de/jdellert/iwsa/corrmodel/train/all_ipa_symbols.csv");
            GeneralizedCorrModelTrainer trainer = new GeneralizedCorrModelTrainer(featureTable, pathToTrainingData, pathToLibSVMData);
            //trainer.createLibSVMData(-1, 1);
            //DecisionTreeModel model = trainer.train("src/main/java/de/jdellert/iwsa/corrmodel/train/model");
            DecisionTreeModel model = trainer.loadModel("src/main/java/de/jdellert/iwsa/corrmodel/train/model");

            Writer wr = new FileWriter("src/main/java/de/jdellert/iwsa/corrmodel/train/inferred_scores_s.tsv");
            List<String> ipaKeyList = new ArrayList<>(featureTable.getFeatureTable().keySet());
            /*Map<String, int[]> filteredMap = featureTable.getFeatureTable().entrySet()
                    .stream()
                    .filter(map -> map.getValue()[2] > -1)
                    .collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue()));
            List<String> ipaKeyList = new ArrayList<>(filteredMap.keySet());*/
            int writtenPairs = 0;
            for (int i = 0; i < ipaKeyList.size(); i++) {
                String sound1 = "s";
                String sound2 = ipaKeyList.get(i);
                int[] featuresAsInt = featureTable.getPair(sound1, sound2);
                double[] features = Arrays.stream(featuresAsInt).asDoubleStream().toArray();
                DenseVector featureVector = new DenseVector(features);
                double score = model.predict(featureVector);
                wr.write(sound1 + "\t" + sound2 + "\t" + score + "\n");
            }

            wr.close();

            // TEST
            /*Map<String, int[]> filteredMap = featureTable.getFeatureTable().entrySet()
                    .stream()
                    .filter(map -> map.getValue()[2] > -1)
                    .collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue()));
            List<String> ipaKeyList = new ArrayList<>(filteredMap.keySet());
            for (int i = 0; i < 100; i++) {
                String sound1 = ipaKeyList.get((int) (Math.random() * ipaKeyList.size()));
                String sound2 = ipaKeyList.get((int) (Math.random() * ipaKeyList.size()));
                int[] featuresAsInt = featureTable.getPair(sound1, sound2);
                double[] features = Arrays.stream(featuresAsInt).asDoubleStream().toArray();
                DenseVector testVector = new DenseVector(features);
                System.out.println("Sound correspondence: " + sound1 + " - " + sound2 + "   SCORE: " + model.predict(testVector));
            }*/


        } catch (IOException e) {
            e.printStackTrace();
        } catch (DataFormatException e) {
            e.printStackTrace();
        }


    }
}
