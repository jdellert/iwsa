package de.jdellert.iwsa.stat;

public class CategoricalDistribution {
	// internal representation: number of observations, sum of observations
	double[] observationCounts;
	double observationCountsSum;

	// actual probabilities are computed based on smoothing
	SmoothingMethod smoothingMethod = SmoothingMethod.NONE;
	double smoothingMassRatio = 0.2; //used for SmoothingMethod.LAPLACE

	public CategoricalDistribution(int k, SmoothingMethod smoothingMethod) {
		this.observationCounts = new double[k];
		this.observationCountsSum = 0.0;
		this.smoothingMethod = smoothingMethod;
	}

	public void setSmoothingMassRatio(double ratio) {
		this.smoothingMassRatio = ratio;
	}

	public void addObservation(int i) {
		observationCounts[i]++;
		observationCountsSum++;
	}

	public double getObservationCount(int i) {
		return observationCounts[i];
	}

	public double getObservationCountsSum() {
		return observationCountsSum;
	}

	public double getProb(int i) {
		switch (smoothingMethod) {
		case NONE:
			return observationCounts[i] / observationCountsSum;
		case LAPLACE:
			return (observationCounts[i] + ((smoothingMassRatio * observationCountsSum) / observationCounts.length))
					/ ((1.0 + smoothingMassRatio) * observationCountsSum);
		default:
			break;
		}
		return 0.0;
	}
}
