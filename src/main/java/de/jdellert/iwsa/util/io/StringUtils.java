package de.jdellert.iwsa.util.io;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class StringUtils {
    public static String multiply(String s, int times) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < times; i++) {
            builder.append(s);
        }
        return builder.toString();
    }

    public static String join(String s, int[] a) {
        return a.length == 0 ? "" : a[0] + (a.length == 1 ? "" : s + join(s, Arrays.copyOfRange(a, 1, a.length)));
    }

    public static String join(String s, Object... a) {
        return a.length == 0 ? "" : a[0] + (a.length == 1 ? "" : s + join(s, Arrays.copyOfRange(a, 1, a.length)));
    }

    public static String join(String s, Set<String> a) {
        if (a == null || a.size() == 0)
            return "";
        return join(s, a.toArray());
    }

    public static String join(String s, List<String> a) {
        if (a == null || a.size() == 0)
            return "";
        return join(s, a.toArray());
    }

    public static String cleanString(Set<String> alphabet, String string) {
        String cleanString = "";
        for (int pos = 0; pos < string.length(); pos++) {
            int endPos = pos + 4;
            if (endPos > string.length())
                endPos = string.length();
            while (endPos > pos) {
                String symbol = string.substring(pos, endPos);
                if (alphabet.contains(symbol) || symbol.equals(" ")) {
                    cleanString += symbol;
                    pos = endPos - 1;
                    break;
                }
                endPos--;
            }
        }
        return cleanString;
    }

    public static String ipaUnicodeForGraphViz(String str) {
        return str.replaceAll("-", "0");
    }

    public static ArrayList<String> getUnigramList(String str) {
        final int n = str.length();
        ArrayList<String> unigrams = new ArrayList<String>(n);
        for (int i = 0; i < n; i++) {
            unigrams.add(str.substring(i, i + 1));
        }
        return unigrams;
    }

    public static ArrayList<String> getBigramList(String str) {
        final int n = str.length() - 1;
        ArrayList<String> bigrams = new ArrayList<String>(n);
        for (int i = 0; i < n; i++) {
            bigrams.add(str.substring(i, i + 2));
        }
        return bigrams;
    }

    public static ArrayList<String> getTrigramList(String str) {
        final int n = str.length() - 2;
        ArrayList<String> trigrams = new ArrayList<String>(n);
        for (int i = 0; i < n; i++) {
            trigrams.add(str.substring(i, i + 3));
        }
        return trigrams;
    }

    public static ArrayList<String> getExtendedBigramList(String str) {
        final int n = str.length() - 2;
        ArrayList<String> extendedBigrams = new ArrayList<String>(n);
        for (int i = 0; i < n; i++) {
            extendedBigrams.add(str.substring(i, i + 1) + "?" + str.substring(i + 2, i + 3));
        }
        return extendedBigrams;
    }

    public static String displayMap(Map<?, ?> map) {
        StringBuilder str = new StringBuilder("{\n");
        for (Entry<?, ?> entry : map.entrySet()) {
            str.append("  " + entry.getKey() + " => " + entry.getValue() + ",\n");
        }
        str.delete(str.length() - 2, str.length());
        str.append("\n}");
        return str.toString();
    }
}
