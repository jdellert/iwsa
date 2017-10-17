package de.jdellert.iwsa.distance;

import de.jdellert.iwsa.align.LevenshteinAlignmentAlgorithm;
import de.jdellert.iwsa.sequence.PhoneticString;

public class EditDistance extends PhoneticStringDistance {

	@Override
	public double normalizedDistance(PhoneticString str1, PhoneticString str2) {
		return LevenshteinAlignmentAlgorithm.computeNormalizedEditDistance(str1, str2);
	}

}
