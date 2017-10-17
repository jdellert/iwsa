package de.jdellert.iwsa.sequence;

import java.util.Arrays;

/**
 * Simple wrapper around int arrays used internally to compactly represent
 * phonetic strings.
 * 
 * @author jdellert
 *
 */

public class PhoneticString {
	public int[] segments;

	public PhoneticString(int[] segments) {
		this.segments = segments;
	}

	public int getLength() {
		return segments.length;
	}

	public String toString(PhoneticSymbolTable symbolTable) {
		return String.join(" ", symbolTable.decode(segments));
	}
}
