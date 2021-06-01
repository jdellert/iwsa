package de.jdellert.iwsa.util.ranking;

/**
 * Wrapper around a key-value pair of types T and Double, comparable by value.
 *
 * @param <T>
 */

public class RankingEntry<T> implements Comparable<RankingEntry<T>> {
    public T key;
    public double value;

    public RankingEntry(T key, double value) {
        this.key = key;
        this.value = value;
    }

    public boolean equals(Object o) {
        if (o instanceof RankingEntry) {
            RankingEntry<T> otherEntry = (RankingEntry<T>) o;
            if (this.value != otherEntry.value)
                return false;
            return this.key.equals(otherEntry.key);
        }
        return false;
    }

    public boolean equals(RankingEntry<T> otherEntry) {
        if (this.value != otherEntry.value)
            return false;
        return this.key.equals(otherEntry.key);
    }

    @Override
    public int compareTo(RankingEntry<T> otherEntry) {
        if (this.value < otherEntry.value)
            return -1;
        if (this.value > otherEntry.value)
            return 1;
        return 0;
    }

    public String toString() {
        return key + "(" + value + ")";
    }

}
