package de.jdellert.iwsa.distance;

import de.jdellert.iwsa.sequence.PhoneticString;

public abstract class PhoneticStringDistance {
	public abstract double normalizedDistance(PhoneticString str1, PhoneticString str2);
}
