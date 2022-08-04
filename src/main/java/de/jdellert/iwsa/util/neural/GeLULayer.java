package de.jdellert.iwsa.util.neural;

import java.io.IOException;
import java.util.InputMismatchException;

public class GeLULayer extends Layer {
    public GeLULayer(int width, String weightsFilepath, String biasesFilepath)
            throws IOException, NumberFormatException, InputMismatchException {
        super(width, width, weightsFilepath, biasesFilepath);
    }

    public GeLULayer(int width, int inputDim, String weightsFilepath, String biasesFilepath)
            throws IOException, NumberFormatException, InputMismatchException {
        super(width, inputDim, weightsFilepath, biasesFilepath);
    }

    /**
     * Gaussian Error function
     * @param x input
     * @return output
     */
    public double erf(double x) {
        double t = 1.0 / (1.0 + 0.47047 * Math.abs(x));
        double poly = t * (0.3480242 + t * (-0.0958798 + t * (0.7478556)));
        double ans = 1.0 - poly * Math.exp(-x*x);
        if (x >= 0) return  ans;
        else        return -ans;
    }

    /**
     * compute Phi by means of erf
     * @param x input
     * @return Phi
     */
    public double phi(double x) {
        return 0.5 * (1.0 + erf(x / (Math.sqrt(2.0))));
    }

    public double[][] gelu(double[][] x) {
        for (int i = 0; i < x.length; i++) {
            for (int j = 0; j < x[0].length; j++) {
                x[i][j] = x[i][j] * phi(x[i][j]);
            }
        }

        return x;
    }

    public double[][] forward(double[][] x) {
        double[][] result = applyWeights(x);
        result = addBias(result);
        result = gelu(result);

        return result;
    }
}
