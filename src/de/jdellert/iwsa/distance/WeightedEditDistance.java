package de.jdellert.iwsa.distance;

import de.jdellert.iwsa.sequence.PhoneticString;

public class WeightedEditDistance extends PhoneticStringDistance {

	@Override
	public double normalizedDistance(PhoneticString str1, PhoneticString str2) {
		return 0.0;
	}

}
