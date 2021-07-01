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
import scala.Tuple2;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.DataFormatException;

public class GeneralizedCorrModelTrainer {
    public GeneralizedCorrModelTrainer() {}

    public void createLIBSVMData(IPAFeatureTable featureTable, String inputFilepath, String outputFilepath,
                                 int lowerPMIFilterBoundary, int upperPMIFilterBoundary) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(inputFilepath));
        Writer writer = new FileWriter(outputFilepath);
        while (br.readLine() != null) {
            String[] fields = br.readLine().split("\t");
            double score = Double.parseDouble(fields[2]);
            if (lowerPMIFilterBoundary < score && score < upperPMIFilterBoundary) {
                continue;
            }
            writer.write(String.valueOf(score));
            int[] sound1Features = featureTable.get(fields[0]);
            int[] sound2Features = featureTable.get(fields[1]);

            if (sound1Features == null || sound2Features == null) {
                continue;
            }
            int[] soundPairFeatures = new int[sound1Features.length * 2];

            // concatenate feature arrays
            System.arraycopy(sound1Features, 0, soundPairFeatures, 0, sound1Features.length);
            System.arraycopy(sound2Features, 0, soundPairFeatures, sound1Features.length, sound2Features.length);

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

    public void train(String pathToLIBSVMFile) {
        SparkConf sparkConf = new SparkConf().setAppName("FeatureGeneralizationsDecisionTree").setMaster("local[2]");
        JavaSparkContext jsc = new JavaSparkContext(sparkConf);
        JavaRDD<LabeledPoint> data = MLUtils.loadLibSVMFile(jsc.sc(), pathToLIBSVMFile).toJavaRDD();
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

        // Save and load model
        model.save(jsc.sc(), "src/main/java/de/jdellert/iwsa/corrmodel/train/tmp/DecisionTreeModel");
    }

    public DecisionTreeModel loadModel(String filePath, JavaSparkContext jsc) {
        return DecisionTreeModel.load(jsc.sc(), filePath);
    }


    public static void main(String[] args) {
        try {
            IPAFeatureTable featureTable = new IPAFeatureTable("src/main/java/de/jdellert/iwsa/corrmodel/train/all_ipa_symbols.csv");
            GeneralizedCorrModelTrainer trainer = new GeneralizedCorrModelTrainer();
            //trainer.createLIBSVMData(featureTable, "src/main/java/de/jdellert/iwsa/corrmodel/train/global-corr-model-output.tsv",
              //      "src/main/java/de/jdellert/iwsa/corrmodel/train/libsvm-corr-data.txt", -1, 1);
            trainer.train("src/main/java/de/jdellert/iwsa/corrmodel/train/libsvm-corr-data.txt");

            // TODO train decision tree on LIBSVM data
            //SparkSession spark = SparkSession.builder().config(sc.getConf()).enableHiveSupport().getOrCreate();
            //Dataset<Row> people = spark.read().parquet("...").as(Encoders.bean(Person.class));

        } catch (IOException e) {
            e.printStackTrace();
        } catch (DataFormatException e) {
            e.printStackTrace();
        }
    }
}
