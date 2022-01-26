package de.jdellert.iwsa.align;

import de.jdellert.iwsa.corrmodel.CorrespondenceModel;
import de.jdellert.iwsa.sequence.PhoneticString;

import java.util.LinkedList;
import java.util.List;

public class NeedlemanWunschAlgorithm {
	public static boolean NEW_DISTANCE_TRANSFORMATION = true;
	
	public static PhoneticStringAlignment constructAlignment(PhoneticString str1, PhoneticString str2,
			CorrespondenceModel gloCorrModel, CorrespondenceModel locCorrModel, CorrespondenceModel selfSimModel1,
			CorrespondenceModel selfSimModel2) {
		int m = str1.getLength() + 1;
		int n = str2.getLength() + 1;

		double[][] mtx = new double[m][n];
		int[][] aSubst = new int[m][n];
		int[][] bSubst = new int[m][n];
		mtx[0][0] = 0;
		for (int i = 1; i < m; i++) {
			mtx[i][0] = mtx[i - 1][0] + getCorrespondenceScore(gloCorrModel, locCorrModel, str1.segments[i - 1], 1);
			aSubst[i][0] = str1.segments[i - 1];
			bSubst[i][0] = 1; // corresponds to gap symbol
		}
		for (int j = 1; j < n; j++) {
			mtx[0][j] = mtx[0][j - 1] + getCorrespondenceScore(gloCorrModel, locCorrModel, 1, str2.segments[j - 1]);
			aSubst[0][j] = 1; // corresponds to gap symbol
			bSubst[0][j] = str2.segments[j - 1];
		}
		for (int i = 1; i < m; i++) {
			for (int j = 1; j < n; j++) {
				double matchValue = mtx[i - 1][j - 1] + getCorrespondenceScore(gloCorrModel, locCorrModel,
						str1.segments[i - 1], str2.segments[j - 1]);
				double insertionValue = mtx[i][j - 1]
						+ getCorrespondenceScore(gloCorrModel, locCorrModel, 1, str2.segments[j - 1]);
				double deletionValue = mtx[i - 1][j]
						+ getCorrespondenceScore(gloCorrModel, locCorrModel, str1.segments[i - 1], 1);
				mtx[i][j] = Math.max(matchValue, Math.max(insertionValue, deletionValue));

				if (insertionValue > matchValue) {
					if (deletionValue > insertionValue) {
						mtx[i][j] = deletionValue;
						aSubst[i][j] = str1.segments[i - 1];
						bSubst[i][j] = 1;
					} else {
						mtx[i][j] = insertionValue;
						aSubst[i][j] = 1;
						bSubst[i][j] = str2.segments[j - 1];
					}
				} else {
					if (deletionValue > matchValue) {
						mtx[i][j] = deletionValue;
						aSubst[i][j] = str1.segments[i - 1];
						bSubst[i][j] = 1;
					} else {
						mtx[i][j] = matchValue;
						aSubst[i][j] = str1.segments[i - 1];
						bSubst[i][j] = str2.segments[j - 1];
					}
				}
			}
		}

		// build the alignment from the backpointer substrings
		int i = m - 1;
		int j = n - 1;
		List<Integer> result1 = new LinkedList<Integer>();
		List<Integer> result2 = new LinkedList<Integer>();
		while (i > 0 || j > 0) {
			int aPart = aSubst[i][j];
			int bPart = bSubst[i][j];
			result1.add(0, aPart);
			result2.add(0, bPart);
			if (aPart != 1)
				i--;
			if (bPart != 1)
				j--;
			if (aPart == 1 && bPart == 1) {
				i--;
				j--;
			}
			if (i < 0 || j < 0)
				break;
		}

		double similarityScore = mtx[m - 1][n - 1];
		double str1SelfSimilarity = 0.0;
		for (int segmentID : str1.segments) {
			str1SelfSimilarity += selfSimModel1.getScore(segmentID, segmentID);
		}
		double str2SelfSimilarity = 0.0;
		for (int segmentID : str2.segments) {
			str2SelfSimilarity += selfSimModel2.getScore(segmentID, segmentID);
		}

		if (NEW_DISTANCE_TRANSFORMATION)
		{
			similarityScore /= result1.size();
			str1SelfSimilarity /= m - 1;
			str2SelfSimilarity /= n - 1;
		}

		double normalizedDistanceScore = 1 - (2 * similarityScore) / (str1SelfSimilarity + str2SelfSimilarity);

		PhoneticStringAlignment alignment = new PhoneticStringAlignment();
		alignment.str1 = new PhoneticString(result1.stream().mapToInt(Integer::intValue).toArray());
		alignment.str2 = new PhoneticString(result2.stream().mapToInt(Integer::intValue).toArray());
		alignment.alignmentScore = similarityScore;
		alignment.normalizedDistanceScore = normalizedDistanceScore;

		return alignment;
	}

	public static double getCorrespondenceScore(CorrespondenceModel gloCorrModel, CorrespondenceModel locCorrModel,
			int ci, int cj) {
		Double score = locCorrModel.getScoreOrNull(ci, cj);
		if (score == null) {
			score = gloCorrModel.getScoreOrNull(ci, cj);
		}
		if (score == null) {
			score = 0.0;
		}
		return score;
	}
}
