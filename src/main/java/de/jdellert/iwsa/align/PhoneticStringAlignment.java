package de.jdellert.iwsa.align;

import de.jdellert.iwsa.sequence.PhoneticString;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;

import java.util.ArrayList;
import java.util.List;

public class PhoneticStringAlignment {
    public double alignmentScore;
    public double normalizedDistanceScore;
    PhoneticString str1;
    PhoneticString str2;

    public String toString(PhoneticSymbolTable symbolTable) {
        return str1.toString(symbolTable) + "\n" + str2.toString(symbolTable);
    }

    public int getLength() {
        return str1.segments.length;
    }

    public int getSymbolPairIDAtPos(int pos, PhoneticSymbolTable symbolTable) {
        return str1.segments[pos] * symbolTable.getSize() + str2.segments[pos];
    }

    public int getSymbol1IDAtPos(int pos) {
        return str1.segments[pos];
    }

    public int getSymbol2IDAtPos(int pos) {
        return str2.segments[pos];
    }

    public List<String[]> getSymbolPairs(PhoneticSymbolTable symbolTable) {
        List<String[]> symbolPairs = new ArrayList<>();
        for (int i = 0; i < getLength(); i++) {
            symbolPairs.add(
                    new String[]{symbolTable.toSymbol(str1.segments[i]), symbolTable.toSymbol(str2.segments[i])});
        }
        return symbolPairs;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PhoneticStringAlignment) {
            PhoneticStringAlignment other = (PhoneticStringAlignment) obj;
            return this.alignmentScore == other.alignmentScore
                    && this.normalizedDistanceScore == other.normalizedDistanceScore && this.str1.equals(other.str1)
                    && this.str2.equals(other.str2);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 5 * (Double.hashCode(alignmentScore)
                + 19 * (Double.hashCode(normalizedDistanceScore) + 37 * (str1.hashCode() + 13 * (str2.hashCode()))));
    }
}
