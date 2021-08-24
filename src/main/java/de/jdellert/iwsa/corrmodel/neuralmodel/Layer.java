package de.jdellert.iwsa.corrmodel.neuralmodel;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.InputMismatchException;

public class Layer {
    private final double[] biases;
    private final double[][] weights;
    private final int width;
    private final int inputDim;

    public Layer(int width, String weightsFilepath, String biasesFilepath)
            throws IOException, NumberFormatException, InputMismatchException {
        this(width, width, weightsFilepath, biasesFilepath);
    }

    public Layer(int width, int inputDim, String weightsFilepath, String biasesFilepath)
            throws IOException, NumberFormatException, InputMismatchException {
        biases = new double[width];
        weights = new double[inputDim][width];
        this.width = width;
        this.inputDim = inputDim;
        loadBiases(biasesFilepath);
        loadWeights(weightsFilepath);
    }

    public void loadWeights (String filepath) throws IOException, NumberFormatException {
        BufferedReader br = new BufferedReader(new FileReader(filepath));
        String line;
        int i = 0;

        while ((line = br.readLine()) != null) {
            if (line.equals("")) {
                break;
            }

            String[] fields = line.split(" ");

            if (fields.length != width || i > inputDim) {
                throw new InputMismatchException("Dimension mismatch: Can not load weights from " + filepath);
            }

            for (int j = 0; j < width; j++) {
                weights[i][j] = Double.parseDouble(fields[j]);
            }

            i++;
        }

        if (i != inputDim) {
            throw new InputMismatchException("Dimension mismatch: Can not load weights from " + filepath);
        }
    }

    public void loadBiases(String filepath) throws IOException, NumberFormatException {
        BufferedReader br = new BufferedReader(new FileReader(filepath));
        String line;
        int i = 0;

        while ((line = br.readLine()) != null) {
            if (line.equals("")) {
                break;
            }

            if (i > width) {
                throw new InputMismatchException("Dimension mismatch: Can not load biases from " + filepath);
            }

            biases[i] = Double.parseDouble(line);
            i++;
        }

        if (i != width) {
            throw new InputMismatchException("Dimension mismatch: Can not load biases from " + filepath);
        }
    }

    /**
     * applies weights on input matrix
     * @param x input matrix of shape numInputs x inputDim
     * @return result matrix of shape numInputs x width
     */
    public double[][] applyWeights(double[][] x) {
        if (x[0].length != inputDim) {
            throw new InputMismatchException("Dimension mismatch: No matrix multiplication possible!");
        }

        int numInputs = x.length;
        double[][] result = new double[numInputs][width];
        for(int i = 0; i < numInputs; i++) {
            for(int j = 0; j < width; j++){
                result[i][j]=0;
                for(int k = 0; k < inputDim; k++){
                    result[i][j] += x[i][k] * weights[k][j];
                }
            }
        }

        return result;
    }

    public double[][] addBias(double[][] x) {
        if (x[0].length != width) {
            throw new InputMismatchException("Dimension mismatch: Biases can not be added!");
        }

        for (int i = 0; i < x.length; i++) {
            for (int j = 0; j < width; j++) {
                x[i][j] = x[i][j] + biases[j];
            }
        }

        return x;
    }

    public double[][] forward(double[][] x) {
        double[][] result = applyWeights(x);
        result = addBias(result);

        return result;
    }
}
