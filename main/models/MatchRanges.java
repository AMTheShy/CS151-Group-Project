package models;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.RandomAccess;

/**
 * A compact, immutable {@code List<MatchRange>} for matches that all have the same length.
 *
 * <p>Why this exists (decision N6): every match of a query has length {@code query.length()},
 * so a list of matches can be stored as a plain {@code int[]} of start offsets (4 bytes per
 * match) instead of millions of {@link MatchRange} objects (about 24 bytes each plus a
 * reference). A one-letter query over 100 MB of English text produces tens of millions of
 * matches; with objects that would be gigabytes of garbage and would break the 1-second search
 * target. {@link #get(int)} creates a {@code MatchRange} on demand.</p>
 *
 * <p>Invariants: the ranges are sorted by start and do not overlap
 * ({@code start[i + 1] >= start[i] + matchLength}) (S3).</p>
 *
 * <p>Mutating methods inherited from {@link java.util.List} throw
 * {@link UnsupportedOperationException}. {@code equals}/{@code hashCode} follow the
 * {@code List} contract, so a {@code MatchRanges} equals a {@code List.of(...)} with the same
 * ranges.</p>
 *
 * <p>Owner: Eric (contract layer, frozen v1.0). The search engine implementation is encouraged
 * to return instances of this class from {@code SearchEngine.findAllMatches}.</p>
 *
 * <p>Threading: Immutable, thread-safe. {@link Builder} is not thread-safe; confine it to one
 * thread.</p>
 */
public final class MatchRanges extends AbstractList<MatchRange> implements RandomAccess {

    private static final int[] NO_STARTS = new int[0];

    /** Sorted start offsets; only the first {@link #size} entries are used. */
    private final int[] starts;

    /** Number of ranges. */
    private final int size;

    /** Length of every range, {@code >= 1}. */
    private final int matchLength;

    private MatchRanges(int[] starts, int size, int matchLength) {
        this.starts = starts;
        this.size = size;
        this.matchLength = matchLength;
    }

    /**
     * Returns an empty list.
     *
     * @param matchLength the length every (non-existing) range would have, {@code >= 1}
     * @return an empty, immutable {@code MatchRanges}
     * @throws IllegalArgumentException if {@code matchLength < 1}
     */
    public static MatchRanges empty(int matchLength) {
        requirePositiveLength(matchLength);
        return new MatchRanges(NO_STARTS, 0, matchLength);
    }

    /**
     * Creates a list from explicit start offsets; convenient in tests.
     *
     * @param matchLength length of every range, {@code >= 1}
     * @param starts      start offsets in ascending, non-overlapping order
     * @return the immutable list (the array is copied)
     * @throws NullPointerException     if {@code starts} is {@code null}
     * @throws IllegalArgumentException if {@code matchLength < 1}, a start is negative, or the
     *                                  ranges are not ascending and non-overlapping
     */
    public static MatchRanges of(int matchLength, int... starts) {
        Builder builder = builder(matchLength);
        for (int start : starts) {
            builder.add(start);
        }
        return builder.build();
    }

    /**
     * Creates a builder; the typical way for a search loop to collect matches.
     *
     * @param matchLength length of every range, {@code >= 1}
     * @return a new, empty builder
     * @throws IllegalArgumentException if {@code matchLength < 1}
     */
    public static Builder builder(int matchLength) {
        requirePositiveLength(matchLength);
        return new Builder(matchLength);
    }

    private static void requirePositiveLength(int matchLength) {
        if (matchLength < 1) {
            throw new IllegalArgumentException("matchLength must be >= 1 but was " + matchLength);
        }
    }

    /**
     * Returns the length shared by all ranges.
     *
     * @return the match length, {@code >= 1}
     */
    public int matchLength() {
        return matchLength;
    }

    /**
     * Returns the start offset of the range at {@code index} without allocating an object.
     *
     * @param index the index, {@code 0 <= index < size()}
     * @return the start offset
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public int startAt(int index) {
        checkIndex(index);
        return starts[index];
    }

    /**
     * Returns the range at {@code index}.
     *
     * @param index the index, {@code 0 <= index < size()}
     * @return a new {@code MatchRange}
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    @Override
    public MatchRange get(int index) {
        checkIndex(index);
        int start = starts[index];
        return new MatchRange(start, start + matchLength);
    }

    /**
     * Returns the number of ranges.
     *
     * @return the size
     */
    @Override
    public int size() {
        return size;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index " + index + " out of bounds for size " + size);
        }
    }

    /**
     * Collects ascending, non-overlapping start offsets into a {@link MatchRanges}.
     *
     * <p>Threading: Not thread-safe; confine to the thread that creates it.</p>
     */
    public static final class Builder {

        private static final int INITIAL_CAPACITY = 16;

        private final int matchLength;
        private int[] starts = new int[INITIAL_CAPACITY];
        private int size;
        private boolean built;

        private Builder(int matchLength) {
            this.matchLength = matchLength;
        }

        /**
         * Appends a range starting at {@code start}.
         *
         * @param start start offset; must be {@code >= 0} and {@code >=} the end of the previously
         *              added range
         * @return this builder
         * @throws IllegalArgumentException if the start is negative or overlaps the previous range
         * @throws IllegalStateException    if {@link #build()} was already called
         */
        public Builder add(int start) {
            if (built) {
                throw new IllegalStateException("build() was already called");
            }
            if (start < 0) {
                throw new IllegalArgumentException("start must be >= 0 but was " + start);
            }
            if (size > 0 && start < starts[size - 1] + matchLength) {
                throw new IllegalArgumentException("ranges must be ascending and non-overlapping: "
                        + start + " < " + (starts[size - 1] + matchLength));
            }
            if (size == starts.length) {
                starts = Arrays.copyOf(starts, Math.max(INITIAL_CAPACITY, starts.length * 2));
            }
            starts[size++] = start;
            return this;
        }

        /**
         * Returns the number of ranges added so far.
         *
         * @return the current size
         */
        public int size() {
            return size;
        }

        /**
         * Builds the immutable list. The builder cannot be used afterwards.
         *
         * @return the list of all added ranges
         * @throws IllegalStateException if called twice
         */
        public MatchRanges build() {
            if (built) {
                throw new IllegalStateException("build() was already called");
            }
            built = true;
            int[] trimmed = size == 0 ? NO_STARTS : Arrays.copyOf(starts, size);
            starts = null;
            return new MatchRanges(trimmed, trimmed.length, matchLength);
        }
    }
}
