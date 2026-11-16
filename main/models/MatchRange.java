package models;

/**
 * A half-open character range {@code [start, end)} inside a text, used for search matches and
 * for highlight positions inside a {@link Snippet} (S3).
 *
 * <p>Invariant: {@code 0 <= start < end}. Empty ranges are not allowed because an empty query is
 * rejected (S2).</p>
 *
 * <p>Natural order: by {@code start}, then by {@code end}.</p>
 *
 * <p>Owner: Eric (contract layer, frozen v1.0).</p>
 *
 * <p>Threading: Immutable, thread-safe.</p>
 *
 * @param start index of the first character of the range (inclusive)
 * @param end   index one past the last character of the range (exclusive)
 */
public record MatchRange(int start, int end) implements Comparable<MatchRange> {

    /**
     * Validates the range.
     *
     * @throws IllegalArgumentException if {@code start < 0} or {@code end <= start}
     */
    public MatchRange {
        if (start < 0) {
            throw new IllegalArgumentException("start must be >= 0 but was " + start);
        }
        if (end <= start) {
            throw new IllegalArgumentException("end must be > start but was [" + start + ", " + end + ")");
        }
    }

    /**
     * Returns the number of characters covered.
     *
     * @return {@code end - start}, always {@code >= 1}
     */
    public int length() {
        return end - start;
    }

    /**
     * Returns this range moved by {@code delta} characters.
     *
     * @param delta the offset to add to both ends; may be negative
     * @return {@code new MatchRange(start + delta, end + delta)}
     * @throws IllegalArgumentException if the moved range would start before 0
     */
    public MatchRange shiftedBy(int delta) {
        return new MatchRange(start + delta, end + delta);
    }

    /**
     * Tells whether this range shares at least one character with {@code [from, to)}.
     *
     * @param from inclusive start of the other interval
     * @param to   exclusive end of the other interval
     * @return {@code true} if {@code start < to && from < end}
     */
    public boolean intersects(int from, int to) {
        return start < to && from < end;
    }

    /**
     * Orders by start, then by end.
     *
     * @param other the range to compare with, not {@code null}
     * @return negative, zero or positive as usual
     */
    @Override
    public int compareTo(MatchRange other) {
        int byStart = Integer.compare(start, other.start);
        return byStart != 0 ? byStart : Integer.compare(end, other.end);
    }
}
