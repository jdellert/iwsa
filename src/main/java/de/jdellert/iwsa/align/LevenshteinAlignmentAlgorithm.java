package de.jdellert.iwsa.align;

import java.util.LinkedList;
import java.util.List;

import de.jdellert.iwsa.sequence.PhoneticString;

public class LevenshteinAlignmentAlgorithm {
	public static double computeNormalizedEditDistance(PhoneticString str1, PhoneticString str2) {
		int m = str1.getLength() + 1;
		int n = str2.getLength() + 1;

		int[][] mtx = new int[m][n];
		mtx[0][0] = 0;
		for (int i = 1; i < m; i++) {
			mtx[i][0] = mtx[i - 1][0] + 1;
		}
		for (int j = 1; j < n; j++) {
			mtx[0][j] = mtx[0][j - 1] + 1;
		}
		for (int i = 1; i < m; i++) {
			for (int j = 1; j < n; j++) {
				int matchValue = mtx[i - 1][j - 1];
				if (str1.segments[i-1] != str2.segments[j-1])
					matchValue++;
				int insertionValue = mtx[i][j - 1] + 1;
				int deletionValue = mtx[i - 1][j] + 1;
				mtx[i][j] = Math.min(matchValue, Math.min(insertionValue, deletionValue));
			}
		}
		return (double) mtx[m - 1][n - 1] / Math.max(m - 1, n - 1);
	}

	public static PhoneticStringAlignment constructAlignment(PhoneticString str1, PhoneticString str2) {
		int m = str1.getLength() + 1;
		int n = str2.getLength() + 1;

		int[][] mtx = new int[m][n];
		int[][] aSubst = new int[m][n];
		int[][] bSubst = new int[m][n];
		mtx[0][0] = 0;
		for (int i = 1; i < m; i++) {
			mtx[i][0] = mtx[i - 1][0] + 1;
			aSubst[i][0] = str1.segments[i - 1];
			bSubst[i][0] = 1; // corresponds to gap symbol
		}
		for (int j = 1; j < n; j++) {
			mtx[0][j] = mtx[0][j - 1] + 1;
			aSubst[0][j] = 1; // corresponds to gap symbol
			bSubst[0][j] = str2.segments[j - 1];
		}
		for (int i = 1; i < m; i++) {
			for (int j = 1; j < n; j++) {

				int matchValue = mtx[i - 1][j - 1];
				if (str1.segments[i-1] != str2.segments[j-1])
					matchValue++;
				int insertionValue = mtx[i][j - 1] + 1;
				int deletionValue = mtx[i - 1][j] + 1;
				mtx[i][j] = Math.min(matchValue, Math.min(insertionValue, deletionValue));

				if (insertionValue < matchValue) {
					if (deletionValue < insertionValue) {
						mtx[i][j] = deletionValue;
						aSubst[i][j] = str1.segments[i - 1];
						bSubst[i][j] = 1;
					} else {
						mtx[i][j] = insertionValue;
						aSubst[i][j] = 1;
						bSubst[i][j] = str2.segments[j - 1];
					}
				} else {
					if (deletionValue < matchValue) {
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

		double alignmentScore = mtx[m - 1][n - 1];
		double normalizedAlignmentScore = alignmentScore / Math.max(m - 1, n - 1);

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

		PhoneticStringAlignment alignment = new PhoneticStringAlignment();
		alignment.str1 = new PhoneticString(result1.stream().mapToInt(Integer::intValue).toArray());
		alignment.str2 = new PhoneticString(result2.stream().mapToInt(Integer::intValue).toArray());
		alignment.alignmentScore = alignmentScore;
		alignment.normalizedDistanceScore = normalizedAlignmentScore;

		return alignment;
	}

}
