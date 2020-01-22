package de.jdellert.iwsa.sequence;

import java.util.Arrays;

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
        return "[" + join(segments, ' ') + "]";
    }

    public String toString(PhoneticSymbolTable symbolTable) {
        return String.join(" ", symbolTable.decode(segments));
    }

    public String toUntokenizedString(PhoneticSymbolTable symbolTable) {
        return String.join("", symbolTable.decode(segments));
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

    private static String join(int[] a, char c) {
        StringBuilder s = new StringBuilder();
        for (int i : a)
            s.append(i).append(c);
        return s.deleteCharAt(s.length()-1).toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PhoneticString)
            return Arrays.equals(this.segments, ((PhoneticString) obj).segments);
        return false;
    }

    @Override
    public int hashCode() {
        return 31 + Arrays.hashCode(this.segments);
    }
}
