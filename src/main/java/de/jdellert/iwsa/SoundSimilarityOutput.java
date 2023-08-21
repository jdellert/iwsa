package de.jdellert.iwsa;

import de.jdellert.iwsa.corrmodel.GeneralizedCorrespondenceModel;
import de.jdellert.iwsa.util.io.SimpleFormatReader;

import java.io.FileWriter;
import java.util.List;

public class SoundSimilarityOutput {
    public static void main(String[] args) {
        try {
            GeneralizedCorrespondenceModel corrModel = new GeneralizedCorrespondenceModel();
            List<String> sounds = SimpleFormatReader.listFromFile(args[0]);
            FileWriter out = new FileWriter(args[1]);
            for (String sound1 : sounds) {
                for (String sound2 : sounds) {
                    out.write(sound1 + "\t" + sound2 + "\t" + corrModel.getScore(sound1, sound2) + "\n");
                }
            }
            out.close();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
