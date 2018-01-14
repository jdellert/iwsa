package de.jdellert.iwsa.stat;

public class CorrespondenceDistribution {
	// internal representation: number of observations, sum of observations
	int k = 0;
	double[] unigramObservations1;
	double[] unigramObservations2;
	
	double[] bigramObservations;
	double observationCountsSum;

	// actual probabilities are computed based on Jelinek-Mercer smoothing
	SmoothingMethod smoothingMethod = SmoothingMethod.NONE;
	double smoothingMassRatio = 0.2; //used for SmoothingMethod.LAPLACE

	public CorrespondenceDistribution(int k) {
		this.k = k;
		this.unigramObservations1 = new double[k];
		this.unigramObservations2 = new double[k];
		this.bigramObservations = new double[k*k];
		this.observationCountsSum = 0.0;
	}
	
	public void addBigramObservation(int bigramID) {
		int i = bigramID / k;
		int j = bigramID % k;
		unigramObservations1[i]++;
		unigramObservations2[j]++;
		bigramObservations[i * k + j]++;
		observationCountsSum++;
	}
	

	public void addBigramObservation(int i, int j) {
		unigramObservations1[i]++;
		unigramObservations2[j]++;
		bigramObservations[i * k + j]++;
		observationCountsSum++;
	}
	
	public double getBigramCount(int bigramID) {
		int i = bigramID / k;
		int j = bigramID % k;
		return getBigramCount(i, j);
	}

	public double getBigramCount(int i, int j) {
		return bigramObservations[i * k + j];
	}

	public double getObservationCountsSum() {
		return observationCountsSum;
	}
	
	public double getProb(int bigramID) {
		int i = bigramID / k;
		int j = bigramID % k;
		return getProb(i, j);
	}

	/**
	 * Returns the probability with Jelinek-Mercer smoothing (interpolation of unigram and bigram models
	 * @param i segmentID in lang1, j segmentID in lang2
	 * @return
	 */
	public double getProb(int i, int j) {
		double sum = observationCountsSum;
		double cij = bigramObservations[i * k + j];
		double ci = unigramObservations1[i];
		double cj = unigramObservations2[j];
		return cij/(cij+1) * (cij/sum) + 1/(cij+1) * (ci/sum) * (cj/sum);
	}
}
