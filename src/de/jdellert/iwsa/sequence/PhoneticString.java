package de.jdellert.iwsa.sequence;

import java.util.Arrays;

import de.jdellert.iwsa.util.io.StringUtils;

/**
 * Simple wrapper around int arrays used internally to compactly represent
 * phonetic strings.
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
	
	public String toString() {
		return "[" + StringUtils.join(" ", segments) + "]";
	}

	public String toString(PhoneticSymbolTable symbolTable) {
		return String.join(" ", symbolTable.decode(segments));
	}

	public PhoneticString copyWithoutGaps() {
		int numGaps = 0;
		for (int segment : segments)
		{
			if (segment == 1) numGaps++;
		}
		int[] reducedSegments = new int[segments.length - numGaps];
		int pos = 0;
		for (int segment : segments)
		{
			if (segment > 1) reducedSegments[pos++] = segment;
		}
		return new PhoneticString(reducedSegments);
	}
}
