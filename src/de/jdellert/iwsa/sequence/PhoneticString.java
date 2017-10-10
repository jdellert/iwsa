package de.jdellert.iwsa.sequence;

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
	// TODO: display method, taking a PhoneticSymbolTable as its argument
}
